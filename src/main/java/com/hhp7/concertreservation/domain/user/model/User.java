package com.hhp7.concertreservation.domain.user.model;

import com.hhp7.concertreservation.domain.point.model.Point;
import java.util.UUID;
import lombok.Getter;

@Getter
public class User {
    private final String id;
    private final String name;

    // 기본 생성자. : Private
    private User(String id, String name) {
        this.id = id;
        this.name = name;
    }


    public static User create(String id, String name){
        return new User(id, name);
    }

    // 실제 생성 시에는 사용자의 이름만 받아 생성하게 되므로 다음과 같은 팩토리 메서드를 정의한다.
    public static User create(String name){
        return new User(
                String.valueOf(UUID.randomUUID())
                , name
        );
    }

    // Domain Model Entity 간 비교를 위해 equals() 구현.
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User user) {
            return this.id.equals(user.id);
        }
        return false;
    }
}
