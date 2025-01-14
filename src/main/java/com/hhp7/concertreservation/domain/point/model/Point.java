package com.hhp7.concertreservation.domain.point.model;

import com.hhp7.concertreservation.exceptions.BusinessRuleViolationException;
import java.util.Objects;

/**
 * Point 개념 그 자체를 표현하는 도메인 모델(VO)입니다.
 * <br></br>
 * Q. 왜 Point 와 PointBalance 모델을 분리했나?
 * <br>
 * A. 포인트 감액/증액 이라는 개념은 '포인트 자체'에 대한 행위라기 보단 '사용자 잔액'에 대한 행위라고 생각하여 분리하였습니다.
 * <br></br>
 * Q. 왜 VO임에도 record 쓰지 않고 class를 썼나?
 * <br>
 * A. record 키워드는 Boilerplate 코드를 많이 줄여주며, 기본적으로 VO로 바로 활용하기 용이하도록 equals(), hashCode(), toString() 과 같은 메서드를 별도 정의없이도 제공해줍니다.
 *   <br>
 *    하지만 record 키워드를 사용할 경우 기본 생성자의 접근 수준을 제한할 수 없습니다.(class의 접근수준이 기본 생성자의 접근 수준에 강제됩니다.)
 *    <br>
 *    하지만 'Point는 음수가 될 수 없다'는 비즈니스 로직을 구현하기 위해선 이러한 기본 생성자에 대한 접근을 제한할 필요가 있었습니다.
 *    <br>
 *    이를 고려하여 다소 간 코드가 길어지는 점을 감수하고 class로 구현하였습니다.
 */
public class Point {

    /** 비즈니스 정책 : 사용자는 0점 이상 1,000,000점 이하의 포인트를 보유할 수 있다.**/
    private static final int MAX_AMOUNT = 1_000_000;
    private static final int MIN_AMOUNT = 0;

    private final int amount;

    /**
     * 인자로 주어진 잔액을 갖는 새로운 {@code Point} 객체 인스턴스를 생성합니다.
     * 이때 잔액에 대한 정책 준수 여부를 검증합니다. (잔액 in [0, 1,000,000])
     *
     * @param amount 생성될 {@code Point} 객체 인스턴스의 잔액량.
     *                    주어진 잔액량은 0 이상 1,000,000 이하여야 한다.
     * @return 주어진 양을 잔액으로 갖는 새로운 {@code Point} 객체 인스턴스.
     * @throws BusinessRuleViolationException 잔액 정책 위반 시 발생하는 예외
     */
    private Point(int amount) {
        this.amount = amount;
    }

    /**
     * 주어진 양을 잔액으로 갖는 새로운 {@code Point} 객체 인스턴스를 생성한다.
     *
     * @param pointAmount 생성될 {@code Point} 객체 인스턴스의 잔액량.
     *                    주어진 잔액량은 0 이상 1,000,000 이하여야 한다.
     * @return 주어진 양을 잔액으로 갖는 새로운 {@code Point} 객체 인스턴스.
     * @throws BusinessRuleViolationException 잔액 정책 위반 시 발생하는 예외
     */
    public static Point create(int pointAmount) {
        if (pointAmount < MIN_AMOUNT) {
            throw new BusinessRuleViolationException("사용자는 0보다 작은 포인트 잔액을 가질 수 없습니다.");
        } else if (pointAmount > MAX_AMOUNT) {
            throw new BusinessRuleViolationException("사용자의 보유 포인트 최대 한도는 1,000,000점 입니다.");
        }
        return new Point(pointAmount);
    }

    /**
     * 현재 포인트 잔액을 반환한다.
     *
     * @return 포인트 잔액
     */
    public int getAmount() {
        return amount;
    }

    /**
     * {@code Point} 객체의 잔액을 증가시킨 새로운 인스턴스를 반환한다.
     *
     * @param increment 증가시킬 포인트 양
     * @return 증가된 포인트 잔액을 가진 새로운 {@code Point} 객체 인스턴스
     * @throws BusinessRuleViolationException 증가 후 잔액이 최대 한도를 초과할 경우 발생
     */
    public Point increase(int increment) {
        int newAmount = this.amount + increment;
        if (newAmount > MAX_AMOUNT) {
            throw new BusinessRuleViolationException("포인트 증액으로 인해 최대 한도를 초과했습니다.");
        }
        return new Point((int) newAmount);
    }

    /**
     * {@code Point} 객체의 잔액을 감소시킨 새로운 인스턴스를 반환한다.
     *
     * @param decrement 감소시킬 포인트 양
     * @return 감소된 포인트 잔액을 가진 새로운 {@code Point} 객체 인스턴스
     * @throws BusinessRuleViolationException 감소 후 잔액이 최소 한도 미만일 경우 발생
     */
    public Point decrease(int decrement) {
        int newAmount = this.amount - decrement;
        if (newAmount < MIN_AMOUNT) {
            throw new BusinessRuleViolationException("포인트 감액으로 인해 잔액이 음수가 될 수 없습니다.");
        }
        return Point.create(newAmount);
    }

    /**
     * {@code Point} 객체의 동등성을 비교한다.
     *
     * @param o 비교 대상 객체
     * @return 두 객체의 잔액이 같으면 {@code true}, 아니면 {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point point)) return false;
        return amount == point.amount;
    }

    /**
     * {@code Point} 객체의 해시 코드를 반환
     *
     * @return 포인트 잔액을 기반으로 한 해시 코드
     */
    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    /**
     * {@code Point} 객체의 문자열 표현을 반환
     *
     * @return 포인트 잔액을 포함한 문자열
     */
    @Override
    public String toString() {
        return "Point{" +
                "amount=" + amount +
                '}';
    }
}