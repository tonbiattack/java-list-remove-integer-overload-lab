# E008: `List<Integer>.remove`が値ではなくインデックスを削除する

## 目的

待機ジョブIDが`[1, 2, 3]`のとき、ID`1`を取消す場合は値`1`を削除し、待機IDを`[2, 3]`、取消履歴を`[1]`にする必要があります。しかし`int`をそのまま`List.remove`へ渡すと、Javaは値削除ではなく位置削除のオーバーロードを選択します。

## 実行環境と再現境界

このラボはJava 21、Maven、JUnit Jupiter 5.11.4だけを使います。フレームワーク、キュー、HTTP、データベース、ファイル、外部I/Oは使いません。公開境界は`PendingJobRegistry#cancel(int)`であり、直接の取消結果に加えて`pendingJobIds()`と`cancellationHistory()`の最終状態を別々に読みます。

テストは固定の待機ID`[1, 2, 3]`に対してID`1`を取消します。ID`1`は値としても、ゼロ始まりの位置としても有効です。この条件により例外が出ずに別の要素が削除されるため、戻り値だけでなく残ったIDと履歴を観測する必要があります。時刻、乱数、並行実行には依存しません。

## 最初に観測した事実

バグ状態はコミット[`65e7d44`](../commit/65e7d44)です。次のコマンドで、意図したアサーション差分を確認しました。

```bash
git checkout 65e7d44
mvn --batch-mode test -Dtest=PendingJobRegistryTest
```

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| 直接の取消結果 | `CANCELLED` | `CANCELLED` | `PendingJobRegistryTest` |
| 待機ジョブID | `[2, 3]` | `[1, 3]` | `PendingJobRegistry#pendingJobIds()` |
| 取消履歴 | `[1]` | `[2]` | `PendingJobRegistry#cancellationHistory()` |
| `remove(1)`の戻り値 | 値削除を意図すると`1` | `2` | `ListRemoveOverloadObservationTest` |
| `remove(Integer.valueOf(1))`の結果 | 値`1`を削除 | `[2, 3]` | `ListRemoveOverloadObservationTest` |

```text
ID 1を取り除き、ID 2と3を待機状態に残す
==> expected: <[2, 3]> but was: <[1, 3]>

取消履歴には要求したID 1を記録する
==> expected: <[1]> but was: <[2]>
```

完全な失敗出力は[`evidence/01-bug-service-test-output.txt`](../evidence/01-bug-service-test-output.txt)に保存しています。取消結果は`CANCELLED`のため、結果コードだけでは誤削除を見つけられません。残存Listと履歴を最終状態として分けて確認したことで、ID`1`ではなくID`2`が削除されていることを確定できました。

## 競合仮説と検証

| 仮説 | 確認方法 | 結果 |
| --- | --- | --- |
| ID`1`が待機Listにない | 取消前の固定Listを確認する | `[1, 2, 3]`なので棄却。 |
| 取消履歴への追記だけが壊れている | `remove`の戻り値と残存Listを直接観測する | 戻り値が`2`、残存Listが`[1, 3]`のため棄却。 |
| `int`引数により`remove(int)`が選ばれる | 同じListで`remove(1)`と`remove(Integer.valueOf(1))`を比較する | 前者は値`2`、後者は値`1`を削除する。採用。 |

## 確定した原因

バグ状態の取消処理は次のとおりでした。

```java
Integer removedJobId = pendingJobIds.remove(jobId);
```

`List`には位置指定の`remove(int index)`と、要素指定の`remove(Object o)`があります。[1] `jobId`は`int`なので、コンパイラは`remove(int)`を選びます。したがって`jobId`が`1`なら、値1ではなく位置1にある値2を削除します。

このケースでは、ID`1`はList内に存在し、位置1も有効なため、例外も`NOT_FOUND`も発生しません。`CANCELLED`という一見正しい結果と誤った状態が共存するため、値を含むListに数値を渡すときはオーバーロードの選択を直接確認する必要があります。

## 最小修正

修正コミットは[`2f6cfbd`](../commit/2f6cfbd)です。削除対象を`Integer`として明示しました。

```java
boolean removed = pendingJobIds.remove(Integer.valueOf(jobId));
```

`Integer.valueOf(jobId)`は`Object`引数の`remove(Object)`を選択します。そのメソッドは一致する最初の要素を削除し、変更があったかをbooleanで返します。[1] 戻り値を確認して、値が存在しない場合は`NOT_FOUND`を返すようにしました。

`jobId`を直接渡すまま履歴だけ修正する、インデックス検索して削除する、テスト期待値を`[1, 3]`へ下げる修正は採用していません。公開契約はIDという**値**を取消すことなので、値削除を明示する一行の変更が適切です。

## 回帰保証

### 再発防止テスト

最初に失敗した`cancel_removesTheRequestedJobIdRatherThanTheSameIndex`はそのまま残しています。このテストは、取消結果、待機ジョブID、取消履歴を別々に検証します。

| テスト | 回帰として守る契約 |
| --- | --- |
| `cancel_removesTheRequestedJobIdRatherThanTheSameIndex` | ID`1`を値として削除し、`[2, 3]`と履歴`[1]`を残す。 |
| `cancel_unknownId_preservesTheQueueAndHistory` | 不明IDの取消では待機IDと履歴を変更しない。 |
| `primitiveIntSelectsIndexRemovalAndBoxedIntegerSelectsValueRemoval` | intとIntegerで別の`remove`オーバーロードが選ばれることを直接示す。 |

修正後の`mvn --batch-mode clean test`では、3テストがすべて成功しました。完全な出力は[`evidence/03-fixed-full-test-output.txt`](../evidence/03-fixed-full-test-output.txt)に保存しています。

## 再現手順

```bash
git checkout 65e7d44
mvn --batch-mode test -Dtest=PendingJobRegistryTest
# expected: <[2, 3]> but was: <[1, 3]>

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

## スコープと注意点

この修正は、数値を**要素値**として削除する場合にだけ有効です。Listの位置を意図的に削除する設計なら`remove(int)`は正しい選択です。識別子と位置を混同しないよう、数値を含むListでは削除APIの引数型を明示してください。

また、このラボは`ArrayList<Integer>`の単純な取消に限定しています。重複ID、並行更新、キューの優先度、永続化、取消権限、範囲外IDといった運用上の責務は別途設計します。

## References

[1] [Oracle: `List` — `remove(int)` and `remove(Object)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html)
