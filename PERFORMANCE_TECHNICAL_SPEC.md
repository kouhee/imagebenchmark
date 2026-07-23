# パフォーマンス計測システム - 技術仕様

## アーキテクチャ

### コンポーネント

```
┌─────────────────────────────────────────────────────┐
│                   MainScreen (UI)                   │
│  - パフォーマンス詳細表示                             │
└────────────────┬────────────────────────────────────┘
                 │
┌─────────────────┴────────────────────────────────────┐
│              MainViewModel                          │
│  - PerformanceAnalyzer (履歴管理)                    │
└────────────────┬────────────────────────────────────┘
                 │
┌─────────────────┴────────────────────────────────────┐
│         ImageProcessorRepositoryImpl                 │
│  - PerformanceTimer による段階計測                    │
└────────────────┬────────────────────────────────────┘
                 │
┌─────────────────┴────────────────────────────────────┐
│            BaseImageProcessor                       │
│  - recordMetrics() による計測記録                    │
│  - 各Processor実装がこれを継承                        │
└──────────────────────────────────────────────────────┘
```

## データ構造

### PerformanceMetrics
```kotlin
data class PerformanceMetrics(
    val totalElapsedMicros: Double,      // 総処理時間(μs)
    val timingMarks: List<TimingMark>    // 段階ごとの時間
)
```

### TimingMark
```kotlin
data class TimingMark(
    val name: String,                   // マーク名
    val elapsedMicros: Double,          // 経過時間(μs)
    val children: List<TimingMark>      // ネストされたマーク
)
```

### PerformanceReport
```kotlin
data class PerformanceReport(
    val label: String,                  // レポートラベル
    val totalTimeUs: Double,            // 総処理時間
    val breakdown: Map<String, Double>, // 段階別時間
    val dimensions: Map<String, Double> // スレッド数など
)
```

### ImageData拡張フィールド
```kotlin
data class ImageData(
    // ... 既存フィールド ...
    var processingTimeUs: Double = 0.0,
    var processingTimeMs: Double = 0.0,
    var processingDetailsUs: Map<String, Double> = emptyMap(),
    var performanceMetrics: PerformanceMetrics? = null
)
```

## 計測フロー

### 1. 処理実行時の計測

```
ProcessImageUseCase.invoke()
  ↓
ImageProcessorRepositoryImpl.process()
  ├─ timer.mark("factory.create")
  ├─ factory.create()
  ├─ timer.mark("processor.process")
  ├─ processor.process() [実装ごと]
  │   ├─ KotlinNaiveGrayScaleProcessor
  │   │   ├─ recordMetrics("parallel_processing")
  │   │   └─ recordDimensions(threadCount, pixelsPerThread)
  │   └─ NativeKotlinNaiveGrayScaleProcessor
  │       └─ recordMetrics("native_grayscale")
  ├─ timer.mark("result.setup")
  └─ result.performanceMetrics = timer.getMetrics()
```

### 2. UI更新フロー

```
MainViewModel.processImage()
  ├─ processImageUseCase.invoke() [上記参照]
  ├─ PerformanceAnalyzer.addReport() [履歴記録]
  └─ _uiState.value = copy(
       performanceMetrics = result.performanceMetrics
     )
        ↓
      MainScreen.kt
        ├─ Text("Elapsed: ${elapsedTimeUs}μs")
        └─ TimingMarks表示ループ
```

## 計測の精度

### 時間単位
- **μs（マイクロ秒）**: 計測の基本単位
- **精度**: System.nanoTime()により最大ナノ秒精度
- **集計**: μs単位（小数点第2位まで表示）

### 計測箇所
1. **Repository層**: ファクトリ生成＋処理実行
2. **Processor層**: 処理時間とスレッド情報
3. **Native層**: JNI呼び出しのオーバーヘッドを含む

## ボトルネック分析への応用

### 1. 段階ごとのパフォーマンス

```
Total: 1234.56μs
├─ Overhead (factory.create + result.setup): 34.56μs (2.8%)
└─ Processing: 1200.00μs (97.2%) ← 最適化対象
```

パーセンテージが90%以上の項目が主なボトルネック。

### 2. スレッド効率

```
理想的な実行時間 = 処理時間 / スレッド数
実際の実行時間 = 計測値
効率 = 理想 / 実際 × 100%
```

効率が50%未満の場合、スレッド間の競合やロード不均衡の可能性。

### 3. 実装比較

```
改善度 = (旧実装 - 新実装) / 旧実装 × 100%
```

ネイティブ実装の効果を定量的に測定。

## パフォーマンスアナライザー

### 機能

```kotlin
class PerformanceAnalyzer {
    fun addReport(report: PerformanceReport)    // レポート追加
    fun getReport(index: Int): PerformanceReport?  // 単一取得
    fun getComparison(i1: Int, i2: Int): String   // 比較
    fun getHistorySummary(): String             // 履歴表示
}
```

### 使用例

```kotlin
// 履歴を表示
val history = viewModel.getPerformanceHistory()
// 出力: "=== History (3 runs) ===\n0: KOTLIN_NAIVE - 1234.56 μs\n..."

// 履歴をクリア
viewModel.clearPerformanceHistory()
```

## 拡張性

### 新しいProcessorの追加

```kotlin
class MyProcessor : BaseImageProcessor() {
    override suspend fun process(image: ImageData): ImageData {
        // 処理実装
        recordMetrics(image, "step1", timeUs1)
        recordMetrics(image, "step2", timeUs2)
        recordDimensions(image, threadCount, pixelsPerThread)
        return image
    }
}
```

### カスタム計測マーク

```kotlin
// UIからアクセス可能な形式でレポート生成
val report = PerformanceReport(
    label = "Custom Analysis",
    totalTimeUs = totalTime,
    breakdown = mapOf("step1" to time1, "step2" to time2),
    dimensions = mapOf("threads" to 8.0)
)
analyzer.addReport(report)
```

## パフォーマンス影響

### 計測オーバーヘッド
- System.nanoTime()呼び出し: < 1μs
- Map生成: < 10μs
- 全体への影響: < 1%

### メモリ使用量
- PerformanceMetrics: ~1KB
- PerformanceReport (各実行): ~1KB
- 履歴100実行: ~100KB

## デバッグ機能

### ログ出力

```kotlin
// ViewModelから
Log.d("Performance", viewModel.getPerformanceHistory())

// 個別レポートの詳細
Log.d("Performance", report.getSummary())
```

### キャッシュクリア

メモリが限定的な環境では定期的に履歴をクリア：

```kotlin
viewModel.clearPerformanceHistory()
```

## 今後の拡張案

1. **CSVエクスポート**: パフォーマンスデータをファイル出力
2. **グラフ表示**: Android Chartsなどで視覚化
3. **自動最適化提案**: 計測結果から最適化案を提示
4. **リモートロギング**: サーバーへのパフォーマンスデータ送信
5. **比較分析**: デバイス間のパフォーマンス比較
