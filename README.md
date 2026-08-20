# `List<Integer>.remove`が値ではなくインデックスを削除する

Java標準ライブラリの`List.remove`を題材に、**整数のジョブIDを取消すつもりで、同じ数値の位置にある別のジョブを削除してしまう**問題を、失敗するテスト、原因の直接観測、最小修正、回帰テストの順に追うデバッグ教材です。既定ブランチの`main`は成功状態に保ち、意図的に失敗する状態はGit履歴に独立して残します。

## この題材で守る契約

> 待機ジョブIDが`[1, 2, 3]`の状態でID`1`の取消を依頼した場合、ID`1`を取り除いて待機IDを`[2, 3]`とし、取消履歴を`[1]`にする。

| 段階 | 実施内容 | 確認すること |
| --- | --- | --- |
| 再現 | `int jobId`をそのまま`pendingJobIds.remove(jobId)`へ渡す | ID`1`ではなくインデックス`1`のID`2`が削除され、待機IDは`[1, 3]`、履歴は`[2]`となる |
| 観測 | 同じListで`remove(1)`と`remove(Integer.valueOf(1))`を比較する | 前者は位置1の`2`を、後者は値`1`を削除する |
| 修正 | `Integer.valueOf(jobId)`を渡す | `remove(Object)`が選択され、要求したIDを削除できる |
| 回帰防止 | 同じ取消テストを再実行する | 取消結果、待機ID、取消履歴がすべて期待どおりになる |

## 必要な環境

| 項目 | バージョン |
| --- | --- |
| JDK | 21 |
| Maven | 3.8以上 |
| テストランナー | JUnit Jupiter 5.11.4 |
| アプリケーションフレームワーク | 不使用 |

## 最短の開始手順

```bash
mvn --batch-mode clean test
```

検証済みの`main`では、3テストがすべて成功します。

## バグを再現する

```bash
git checkout 65e7d44
mvn --batch-mode test -Dtest=PendingJobRegistryTest
# expected: <[2, 3]> but was: <[1, 3]>
# expected: <[1]> but was: <[2]>

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

バグコミットではIDの存在確認や取消結果ではなく、要求したIDを削除する状態契約だけが失敗します。完全な出力は[`evidence/01-bug-service-test-output.txt`](evidence/01-bug-service-test-output.txt)に保存しています。

## 原因の要点

`List`には二つの`remove`オーバーロードがあります。`remove(int index)`は指定位置の要素を取り除き、`remove(Object o)`は一致する最初の要素を取り除きます。[1] `int`型の`jobId`を渡すと前者が選択されます。

そのため、`pendingJobIds.remove(1)`は値`1`を削除するのではなく、ゼロ始まりの位置1にある値`2`を削除します。`Integer.valueOf(jobId)`で明示的にboxすれば`remove(Object)`が選択され、ID値として削除できます。[1]

## プロジェクト構成

```text
.
├── docs/
│   ├── debugging-record.md      # 観測・仮説・原因・修正・回帰保証
│   ├── novelty-report.md        # 既存Java記事との四軸比較
│   └── topic-brief.md           # 実装前に固定した契約と再現境界
├── evidence/
│   ├── 01-bug-service-test-output.txt
│   ├── 02-list-overload-observation-output.txt
│   └── 03-fixed-full-test-output.txt
├── src/main/java/.../jobs/
│   ├── CancellationOutcome.java
│   └── PendingJobRegistry.java
└── src/test/java/.../jobs/
    ├── ListRemoveOverloadObservationTest.java
    └── PendingJobRegistryTest.java
```

詳細な調査手順は[デバッグ記録](docs/debugging-record.md)、既存コンテンツとの差分は[題材重複調査レポート](docs/novelty-report.md)を参照してください。

## スコープ

この教材は既知のIDを持つ`ArrayList<Integer>`の単純な取消を対象にします。ジョブ実行、並行処理、キュー実装、永続化、重複ID、範囲外ID、負値、取消権限は対象外です。

`List`上の位置を意図的に削除する設計なら、`remove(int)`は正しい選択です。値と位置のどちらを削除したいかをAPI呼び出しの型で明確にし、数値を持つListでは特に注意してください。

## References

[1] [Oracle: `List` — `remove(int)` and `remove(Object)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html)
