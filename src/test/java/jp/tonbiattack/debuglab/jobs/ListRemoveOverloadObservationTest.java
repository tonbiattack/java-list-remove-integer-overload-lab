package jp.tonbiattack.debuglab.jobs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListRemoveOverloadObservationTest {

    @Test
    void primitiveIntSelectsIndexRemovalAndBoxedIntegerSelectsValueRemoval() {
        List<Integer> indexRemoval = new ArrayList<>(List.of(1, 2, 3));
        List<Integer> valueRemoval = new ArrayList<>(List.of(1, 2, 3));

        Integer removedAtIndex = indexRemoval.remove(1);
        boolean removedByValue = valueRemoval.remove(Integer.valueOf(1));

        assertAll(
                () -> assertEquals(2, removedAtIndex,
                        "intの1は値1ではなくインデックス1の要素を取り除く"),
                () -> assertEquals(List.of(1, 3), indexRemoval),
                () -> assertTrue(removedByValue,
                        "Integerへ明示的にboxすると値1の削除を要求できる"),
                () -> assertEquals(List.of(2, 3), valueRemoval)
        );
    }
}
