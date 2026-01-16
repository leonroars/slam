package com.slam.concertreservation.component.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ResponseIdempotencyCodecUnitTest {

    private ResponseIdempotencyCodec codec;
    private ObjectMapper objectMapper;

    // 테스트용 DTO
    record SampleDto(String id, String name, int value) {}

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        codec = new ResponseIdempotencyCodec(objectMapper);
    }

    @Nested
    @DisplayName("encode() 테스트")
    class EncodeTest {

        @Test
        @DisplayName("성공 : Body가 있는 ResponseEntity를 정상적으로 인코딩한다")
        void shouldEncodeResponseEntityWithBody() {
            // given
            SampleDto body = new SampleDto("1", "test", 100);
            ResponseEntity<SampleDto> response = ResponseEntity.ok(body);

            // when
            IdempotencyRecord record = codec.encode(response);

            // then
            assertThat(record).isNotNull();
            assertThat(record.getHttpStatusCode().value()).isEqualTo(200);
            assertThat(record.getBody()).isNotNull();
            assertThat(record.getBody()).contains("\"id\":\"1\"");
            assertThat(record.getBody()).contains("\"name\":\"test\"");
            assertThat(record.getBody()).contains("\"value\":100");
            assertThat(record.getBodyType()).isEqualTo(SampleDto.class.getName());
        }

        @Test
        @DisplayName("성공 : Body가 없는 ResponseEntity를 정상적으로 인코딩한다")
        void shouldEncodeResponseEntityWithoutBody() {
            // given
            ResponseEntity<Void> response = ResponseEntity.noContent().build();

            // when
            IdempotencyRecord record = codec.encode(response);

            // then
            assertThat(record).isNotNull();
            assertThat(record.getHttpStatusCode().value()).isEqualTo(204);
            assertThat(record.getBody()).isNull();
            assertThat(record.getBodyType()).isNull();
        }

        @Test
        @DisplayName("성공 : 다양한 HTTP Status 코드를 정상적으로 인코딩한다")
        void shouldEncodeVariousHttpStatusCodes() {
            // given
            SampleDto body = new SampleDto("1", "test", 100);

            // when & then - 201 Created
            ResponseEntity<SampleDto> created = ResponseEntity.status(HttpStatus.CREATED).body(body);
            IdempotencyRecord createdRecord = codec.encode(created);
            assertThat(createdRecord.getHttpStatusCode().value()).isEqualTo(201);

            // when & then - 202 Accepted
            ResponseEntity<SampleDto> accepted = ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
            IdempotencyRecord acceptedRecord = codec.encode(accepted);
            assertThat(acceptedRecord.getHttpStatusCode().value()).isEqualTo(202);
        }

        @Test
        @DisplayName("성공 : Body가 null인 ResponseEntity를 정상적으로 인코딩한다")
        void shouldEncodeResponseEntityWithNullBody() {
            // given
            ResponseEntity<SampleDto> response = ResponseEntity.ok(null);

            // when
            IdempotencyRecord record = codec.encode(response);

            // then
            assertThat(record).isNotNull();
            assertThat(record.getHttpStatusCode().value()).isEqualTo(200);
            assertThat(record.getBody()).isNull();
            assertThat(record.getBodyType()).isNull();
        }
    }

    @Nested
    @DisplayName("decode() 테스트")
    class DecodeTest {

        @Test
        @DisplayName("성공 : Body가 있는 IdempotencyRecord를 정상적으로 디코딩한다")
        void shouldDecodeRecordWithBody() {
            // given
            String bodyJson = "{\"id\":\"1\",\"name\":\"test\",\"value\":100}";
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .httpStatusCode(200)
                    .body(bodyJson)
                    .bodyType(SampleDto.class.getName())
                    .status(IdempotencyRecordStatus.COMPLETED)
                    .build();

            // when
            ResponseEntity<Object> response = codec.decode(record);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isInstanceOf(SampleDto.class);

            SampleDto decodedBody = (SampleDto) response.getBody();
            assertThat(decodedBody.id()).isEqualTo("1");
            assertThat(decodedBody.name()).isEqualTo("test");
            assertThat(decodedBody.value()).isEqualTo(100);
        }

        @Test
        @DisplayName("성공 : Body가 null인 IdempotencyRecord를 정상적으로 디코딩한다")
        void shouldDecodeRecordWithNullBody() {
            // given
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .httpStatusCode(204)
                    .body(null)
                    .bodyType(null)
                    .status(IdempotencyRecordStatus.COMPLETED)
                    .build();

            // when
            ResponseEntity<Object> response = codec.decode(record);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("성공 : bodyType만 null인 경우 body도 null로 처리한다")
        void shouldReturnNullBodyWhenBodyTypeIsNull() {
            // given
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .httpStatusCode(200)
                    .body("{\"id\":\"1\"}")  // body는 있지만
                    .bodyType(null)          // bodyType이 null
                    .status(IdempotencyRecordStatus.COMPLETED)
                    .build();

            // when
            ResponseEntity<Object> response = codec.decode(record);

            // then
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 클래스 타입으로 디코딩하면 RuntimeException이 발생한다")
        void shouldThrowExceptionWhenClassNotFound() {
            // given
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .httpStatusCode(200)
                    .body("{\"id\":\"1\"}")
                    .bodyType("com.nonexistent.FakeClass")
                    .status(IdempotencyRecordStatus.COMPLETED)
                    .build();

            // when & then
            assertThatThrownBy(() -> codec.decode(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("디코딩에 실패");
        }

        @Test
        @DisplayName("실패 : 잘못된 JSON 형식으로 디코딩하면 RuntimeException이 발생한다")
        void shouldThrowExceptionWhenInvalidJson() {
            // given
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .httpStatusCode(200)
                    .body("invalid json {{{")
                    .bodyType(SampleDto.class.getName())
                    .status(IdempotencyRecordStatus.COMPLETED)
                    .build();

            // when & then
            assertThatThrownBy(() -> codec.decode(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("디코딩에 실패");
        }
    }

    @Nested
    @DisplayName("encode-decode 라운드트립 테스트")
    class RoundTripTest {

        @Test
        @DisplayName("성공 : encode 후 decode하면 원본과 동일한 데이터를 복원한다")
        void shouldRestoreOriginalDataAfterRoundTrip() {
            // given
            SampleDto originalBody = new SampleDto("123", "round-trip-test", 999);
            ResponseEntity<SampleDto> originalResponse = ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(originalBody);

            // when
            IdempotencyRecord record = codec.encode(originalResponse);
            ResponseEntity<Object> decodedResponse = codec.decode(record);

            // then
            assertThat(decodedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            SampleDto decodedBody = (SampleDto) decodedResponse.getBody();
            assertThat(decodedBody).isNotNull();
            assertThat(decodedBody.id()).isEqualTo(originalBody.id());
            assertThat(decodedBody.name()).isEqualTo(originalBody.name());
            assertThat(decodedBody.value()).isEqualTo(originalBody.value());
        }

        @Test
        @DisplayName("성공 : Body가 없는 ResponseEntity도 라운드트립이 정상 동작한다")
        void shouldHandleRoundTripWithNoBody() {
            // given
            ResponseEntity<Void> originalResponse = ResponseEntity.noContent().build();

            // when
            IdempotencyRecord record = codec.encode(originalResponse);
            ResponseEntity<Object> decodedResponse = codec.decode(record);

            // then
            assertThat(decodedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(decodedResponse.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("bodyType 저장 방식 검증")
    class BodyTypeTest {

        @Test
        @DisplayName("성공 : bodyType은 FQCN(Fully Qualified Class Name)으로 저장된다")
        void shouldStoreBodyTypeAsFQCN() {
            // given
            SampleDto body = new SampleDto("1", "test", 100);
            ResponseEntity<SampleDto> response = ResponseEntity.ok(body);

            // when
            IdempotencyRecord record = codec.encode(response);

            // then
            String expectedFQCN = "com.slam.concertreservation.component.idempotency.ResponseIdempotencyCodecUnitTest$SampleDto";
            assertThat(record.getBodyType()).isEqualTo(expectedFQCN);
        }

        @Test
        @DisplayName("주의 : List 타입은 ArrayList로 저장되어 제네릭 정보가 소실된다")
        void shouldLoseGenericInfoForListType() {
            // given
            List<SampleDto> bodyList = List.of(
                    new SampleDto("1", "first", 100),
                    new SampleDto("2", "second", 200)
            );
            ResponseEntity<List<SampleDto>> response = ResponseEntity.ok(bodyList);

            // when
            IdempotencyRecord record = codec.encode(response);

            // then
            // List.of()는 ImmutableCollections$ListN을 반환하므로 실제 구현 클래스가 저장됨
            // 이는 제네릭 타입 정보(SampleDto)가 소실됨을 의미
            assertThat(record.getBodyType()).doesNotContain("SampleDto");
            assertThat(record.getBodyType()).contains("List");
        }
    }
}
