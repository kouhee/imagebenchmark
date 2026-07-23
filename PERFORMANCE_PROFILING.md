# パフォーマンスプロファイリング ガイド

## 概要

ImageBenchMarkプロジェクトには、処理時間のボトルネック分析を容易にする詳細なパフォーマンス計測機能が組み込まれています。

## 計測機能

### 1. Repository層の計測

すべての画像処理は自動的に以下の段階で計測されます：

- **factory.create**: Processorファクトリの生成時間
- **processor.process**: 実際の画像処理時間
- **result.setup**: 結果セットアップ時間（processingTimeMs等の代入）
- **全体**: 処理開始から終了までの総時間

### 2. Processor内部の詳細計測

各Processorの内部処理も段階ごとに計測されます：

#### KotlinNaiveGrayScaleProcessor
```
KotlinNaiveGrayScale (総時間)
├── initialization (初期化処理)
└── parallel_pixel_processing (並列ピクセル処理)
```

#### NativeKotlinNaiveGrayScaleProcessor
```
NativeGrayScale (総時間)
└── jni_call (JNI呼び出し)
```

### 3. UI表示

処理完了後、画面に以下が表示されます：

```
Elapsed : XXX.XXX μs (全体処理時間)

Performance Details (μs):
  Total: XXX.XX μs
  factory.create: XX.XX μs
  processor.process: XXX.XX μs
  result.setup: X.XX μs

Processing Phases:
KotlinNaiveGrayScale: XXX.XXX μs
  initialization: X.XXX μs
  parallel_pixel_processing: XXX.XXX μs
```

## ボトルネック分析方法

### ステップ1: 複数回の実行

同じ画像で複数回処理を実行してデータを収集します：

1. 画像を読み込む
2. **実行**ボタンをクリック（複数回繰り返す）
3. 各実行のパフォーマンス詳細を確認

### ステップ2: 異なるエンジンの比較

同じ画像で異なるProcessingEngineを実行して比較：

1. **Filter** と **Engine** を変更
2. **実行**ボタンをクリック
3. 処理時間とパフォーマンス詳細を比較

例：
- `KOTLIN_NAIVE` vs `NATIVE_NAIVE`
- 並列処理のコスト vs ネイティブ実装のコスト

### ステップ3: 処理段階の分析

各エンジンの処理段階ごとの時間分布を観察：

**KotlinNaiveGrayScaleProcessor:**
- initialization: テーブル設定、スレッド数計算
- parallel_pixel_processing: 実際のピクセル処理

**NativeKotlinNaiveGrayScaleProcessor:**
- jni_call: C++ネイティブ実装の実行時間

### ステップ4: 画像サイズの影響測定

異なるサイズの画像で処理時間の変化を観察：

- 小さい画像（例：1024x768）
- 中程度の画像（例：2048x1536）
- 大きい画像（例：4096x3072）

### パフォーマンス計測データの解釈

#### Repository層の内訳

```
Total: 1234.56μs (100%)
  ├─ factory.create: 10.00μs (0.8%)
  ├─ processor.process: 1210.00μs (98.0%) ← 最適化対象
  └─ result.setup: 14.56μs (1.2%)
```

#### Processor内部の内訳

```
KotlinNaiveGrayScale: 1210.00μs (100%)
  ├─ initialization: 5.00μs (0.4%)
  └─ parallel_pixel_processing: 1205.00μs (99.6%) ← ボトルネック
```

パーセンテージが高い項目がボトルネックです。

#### スレッド情報

```
threadCount: 8 (使用スレッド数)
pixelsPerThread: 262144 (スレッドあたりのピクセル数)
```

## 最適化のポイント

### 1. Processor選択

```
KotlinNaive処理時間 vs Native処理時間
→ ネイティブ実装が60%以上高速化できるかを確認
```

### 2. 初期化コストの削減

```
初期化時間 = initialization + factory.create
→ 小さい画像処理の場合、全体に占める割合が高い
→ キャッシング戦略の検討
```

### 3. 並列処理の効率

```
理想的な実行時間 = pixel_processing時間 / スレッド数
実際の実行時間 = 計測値
効率 = 理想 / 実際 × 100%
```

効率が50%未満の場合、スレッド間の競合やロード不均衡の可能性。

## ViewModelでのプログラマティックアクセス

### 実行履歴の取得

```kotlin
val history = viewModel.getPerformanceHistory()
Log.d("Performance", history)
```

出力例：
```
=== History (3 runs) ===
0: KOTLIN_NAIVE - 1234.56 μs
1: KOTLIN_NAIVE - 1210.34 μs
2: NATIVE_NAIVE - 340.12 μs
```

### 履歴のクリア

```kotlin
viewModel.clearPerformanceHistory()
```

## トラブルシューティング

### Processing Phasesが表示されない

1. アプリが最新版かを確認
2. 処理が完了するまで待機
3. Processor実装がProcessingPhaseRecorderを使用しているか確認

### 処理時間がばらつく

- **原因**: システム負荷、GCの実行、キャッシュ効果
- **対策**: 複数回実行して平均値を取得

### 初期化時間が長い

- **原因**: テーブル構築、スレッドプール初期化
- **対策**: 初回実行をスキップして計測

## カスタム計測の追加

新しいProcessorで計測を追加する場合：

```kotlin
class MyProcessor : BaseImageProcessor() {
    override suspend fun process(image: ImageData): ImageData {
        val recorder = ProcessingPhaseRecorder()
        recorder.startPhase("MyProcessor")

        // ステップ1
        recorder.startPhase("step1")
        // 処理実装
        recorder.endPhase()
        recorder.endCurrent()

        // ステップ2
        recorder.startPhase("step2")
        // 処理実装
        recorder.endPhase()
        recorder.endCurrent()

        recorder.endPhase()
        image.processingPhases = recorder.getRoot()
        
        return image
    }
}
```

## まとめ

このプロファイリング機能により：

✅ Repository層のオーバーヘッドを定量測定  
✅ Processor内部の段階ごとのボトルネック特定  
✅ 異なる実装の効果を定量的に比較  
✅ スレッド効率を把握  
✅ 最適化の効果を即座に確認  

を実現できます。

