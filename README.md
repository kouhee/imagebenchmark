# ImageBenchMark

Androidにおける画像処理エンジンのパフォーマンスを比較・計測するためのプロジェクトです。
Kotlin、Native (C++ / NEON)、GPU (RenderEffect / RuntimeShader) の各種実装の速度差をプロファイラーで詳細に分析できます。

## パフォーマンス計測の手順 (CPU Profiler / System Trace)

プロジェクト内に組み込まれた `CoroutineName` と `android.os.Trace` により、Android Studio の CPU Profiler 上で各エンジンの動作を詳細に追跡できます。

### 1. プロファイラーの起動
1. Android Studio でアプリを実行します。
2. 下部の **[Profiler]** タブを開き、実行中のプロセスを選択して **[CPU]** タイムラインをクリックします。

### 2. レコーディングの開始
1. **[Record]** ボタンの隣にあるドロップダウンから **[System Trace]** を選択します。
   * ※重要: `Java/Kotlin Method Sample` ではなく、必ず `System Trace` を選択してください。これによりコルーチン名や `android.os.Trace` のラベルが表示されます。
2. **[Record]** をクリックして計測を開始します。

### 3. アプリの操作
1. アプリ上で計測したいエンジン（例: Native Sobel, GPU RenderEffect など）を選択します。
2. 画像処理を実行します。

### 4. 解析と確認
1. 処理が終わったらプロファイラーで **[Stop]** をクリックします。
2. 解析完了後、以下のセクションを確認してください：

#### A. Threads セクション
各スレッドの名前に、実装に応じた識別子が表示されます。
- `NativeWorker`: C++スレッドプールによる並列処理
- `BasicGrayScale`, `NativeSobel` など: 各エンジンのメインコルーチン
- `KotlinNaiveGrayScale_Worker_N`: Kotlin側での並列処理ワーカースレッド

#### B. User Annotation / Display セクション
タイムライン上に色付きのバーが表示されます。
- `NativeSobel_Process`, `GPURenderEffectSobel_Process` などのラベルを探してください。
- バーの長さを確認することで、純粋な処理時間をミリ秒単位で把握できます。

## 実装されているエンジン
- **Kotlin Basic**: シンプルなループによる処理。
- **Kotlin Naive/Interpolated**: `IntArray` のテーブル参照や、補間（Interpolation）を用いた最適化。
- **Native (C++)**: NDK を使用した C++ 実装。NEON 命令による高速化。
- **GPU (RenderEffect)**: API 31+ の `RenderEffect` や API 33+ の `RuntimeShader` (SkSL) を使用したハードウェア加速。

---
このプロジェクトは、Android のパフォーマンスチューニングの学習およびベンチマークのために作成されました。


## 取得

./get_perf_trace.ps1

## 保存

adb -s adb-38301FDJG008F2-nmkcrw._adb-tls-connect._tcp pull /data/misc/perfetto-traces/trace.perfetto-trace ./trace_GrayScale_Native-2.perfetto-trace