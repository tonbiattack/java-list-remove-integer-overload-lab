# 題材重複調査レポート: `List<Integer>.remove`が値ではなくインデックスを削除する

## 調査対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 難易度プロファイル | 実践・上級 |
| 候補題材 | 待機ID`[1, 2, 3]`からID`1`を取消すとき、`List<Integer>.remove(1)`が値1でなく位置1の値2を削除する問題 |
| 観測可能な契約 | ID1の取消後に`[2, 3]`と履歴`[1]`を残すべきだが、バグ状態では`[1, 3]`と履歴`[2]`になる。 |
| 直接原因 | `int`引数が`List.remove(int)`を選択し、整数値を位置として扱うこと。 |
| カタログ更新日時 | Repository Catalog（`/home/ubuntu/repository-catalog`）は存在しなかったため更新・検証は実施不能。代替として、ユーザー指定の`tonbiattack/qiita`を取得し、Java・`List.remove`・整数削除・インデックス削除・オーバーロードを本文とパスから検索した。 |
| 検索語 | `Java`, `List.remove`, `remove(int)`, `remove(Integer)`, `インデックス削除`, `整数削除`, `オーバーロード` |

Repository Catalogが利用できなかったため、このレポートの新規性判断は指定コンテンツリポジトリを対象とします。カタログに未登録のローカル専用教材までは保証できない限界を明示します。

## 近接候補の比較

| 既存コンテンツ | 既存の原因 | 既存の実境界・最終観測 | 今回の差分 | 判定 |
| --- | --- | --- | --- | --- |
| 「TreeListのIterator削除後にpreviousが古いノードを返す」 | Iteratorの`remove`後にAVLノードへの`next`キャッシュを無効化していない。 | `ListIterator`で要素を削除し、続く`previous()`が返す要素を観測する。 | 既存はApache Commons CollectionsのTreeList内部キャッシュと構造変更後のイテレータ、今回はJDK `ArrayList<Integer>`のオーバーロード解決を扱う。実境界は状態を持つIteratorかID取消APIか、原因はキャッシュ鮮度かint／Object選択か、修正はキャッシュ無効化かboxingかという四軸で異なる。 | 重複なし |
| 「prefixMap.clearで最後の要素を削除するとNullPointerExceptionになる」 | カスタムprefixMapの最後の要素削除後の状態処理に不備がある。 | 最後の要素を削除して例外とMap状態を観測する。 | 既存はカスタムMapの末尾削除とnull参照、今回は標準Listの数値オーバーロードと誤削除を扱う。 | 重複なし |
| 「PriorityQueueを反復してバッチ抽出し、優先度の低いチケットを先に配信する」 | iterator／stream順を優先順と誤認している。 | バッチ結果、配信履歴、待機キューを観測する。 | 先行教材はPriorityQueueの抽出順、今回はListの削除対象の型を扱う。 | 重複なし |
| 「Map.getOrDefaultがnull値を既定リージョンへ置き換えない」 | nullマッピングを既定値へ置換すると誤認している。 | 解決値、最後のリージョン、既定利用件数を観測する。 | 先行教材はMap値のnull正規化、今回はList要素と位置のオーバーロードを扱う。 | 重複なし |
| 「Collectors.toMapが重複SKUで失敗し、価格スナップショットを公開できない」 | Collectorに重複キーのマージ関数がない。 | 公開結果、スナップショット、公開バージョンを観測する。 | 先行教材はStreamのMap集約、今回はListの単一削除を扱う。 | 重複なし |
| 「String.splitが末尾の空列を捨て、CSVインポートが任意列を拒否する」 | 既定limitが末尾空トークンを破棄する。 | 入力の受理結果、保存行、拒否件数を観測する。 | 先行教材はStringの分割規則、今回はListの削除オーバーロードを扱う。 | 重複なし |

## 結論

**作成する。**

リスト削除に近接するTreeListのIterator記事は確認できましたが、`List<Integer>.remove`の**int／Objectオーバーロードが別の要素を削除する**ことを直接原因として、取消結果・残存ID・削除履歴を扱う記事・教材は見つかりませんでした。

既存のTreeList記事とは、カスタム実装の構造変更後キャッシュか、JDK APIのオーバーロード選択かが異なります。実境界、観測契約、最小修正も独立しているため、教育上意味のある追加題材と判断しました。

## 作成前チェック

- [ ] Repository Catalogを手動更新して検証した。利用可能な`/home/ubuntu/repository-catalog`が存在しなかったため未実施。
- [x] 代替として`tonbiattack/qiita`を取得し、JavaのList・削除・整数・オーバーロードに関する語彙的な近接候補を抽出した。
- [x] 高近接のTreeList Iterator削除記事本文を確認し、四軸で比較した。
- [x] 先行するWebhook、`String.split`、`Collectors.toMap`、PriorityQueue、正規表現置換、URI解決、Map null値教材とも、直接原因・実境界・観測契約・最小修正を比較した。
- [x] 同じ失敗を名称だけ変えて再実装していない。
- [x] `language-agnostic-debugging-lab`の品質ゲートに沿い、失敗テスト、原因観測、最小修正、回帰テスト、バグ・修正の分離コミットを実装した。
