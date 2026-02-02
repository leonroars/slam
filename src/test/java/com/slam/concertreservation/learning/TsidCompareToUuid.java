package com.slam.concertreservation.learning;

import io.hypersistence.tsid.TSID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TsidCompareToUuid {

    @Test
    @DisplayName("TSID는 Time-Sorted 특성을 가진다.")
    void tsidShouldBeTimeSorted() throws InterruptedException {
        // given
        List<TSID> tsidList = new ArrayList<>();
        int count = 10;

        // when
        for (int i = 0; i < count; i++) {
            tsidList.add(TSID.fast());
            Thread.sleep(10); // 시간 차이를 두기 위해 잠시 대기
        }

        List<TSID> sortedList = new ArrayList<>(tsidList);
        Collections.sort(sortedList);

        // then
        // 생성된 순서가 곧 정렬된 순서여야 함
        assertThat(tsidList).isEqualTo(sortedList);
    }

    @Test
    @DisplayName("TSID(Long)은 UUID(String)보다 저장 공간 효율이 좋다 (길이 비교)")
    void tsidShouldBeMoreCompactThanUuid() {
        // given
        TSID tsid = TSID.fast();
        UUID uuid = UUID.randomUUID();

        // when
        long tsidLong = tsid.toLong();
        String tsidString = tsid.toString(); // Base62 encoded
        String uuidString = uuid.toString();

        // then
        System.out.println("TSID (Long): " + tsidLong);
        System.out.println("TSID (Base62): " + tsidString + " (Length: " + tsidString.length() + ")");
        System.out.println("UUID: " + uuidString + " (Length: " + uuidString.length() + ")");

        // String 표현형으로 비교해도 TSID(13자)가 UUID(36자)보다 짧음
        assertThat(tsidString.length()).isLessThan(uuidString.length());
        assertThat(tsidString.length()).isEqualTo(13);
    }
}
