package com.example.backend.architecture

import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * 헥사고날 + 도메인 분리 경계를 테스트로 강제한다(컴파일 대신 CI 로 위반 차단).
 *
 * 도메인(user/place/course)은 서로의 내부에 의존하지 않고,
 * 오직 상대 도메인의 application.port.inbound(공개 API)만 참조할 수 있다.
 * BFF 패키지(bff)는 화면단위 조합을 위해 도메인의 inbound 포트에 의존할 수 있다.
 */
class HexagonalArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.example.backend")

    @Test
    fun `도메인은 애플리케이션·어댑터·스프링·Exposed 에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..application..",
                "..adapter..",
                "org.springframework..",
                "org.jetbrains.exposed..",
            ).check(classes)
    }

    @Test
    fun `애플리케이션은 어댑터에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .check(classes)
    }

    @Test
    fun `웹(inbound) 어댑터는 영속성(outbound) 어댑터에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..adapter.inbound..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter.outbound..")
            .check(classes)
    }

    @Test
    fun `영속성(outbound) 어댑터는 웹(inbound) 어댑터에 의존하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..adapter.outbound..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter.inbound..")
            .check(classes)
    }

    @Test
    fun `어댑터는 서비스 구현체가 아니라 포트에 의존한다`() {
        noClasses()
            .that()
            .resideInAPackage("..adapter..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..application.service..")
            .check(classes)
    }

    /**
     * 포트 계약 DTO는 application.port.inbound(.dto)에 둔다 — application.dto 재도입 차단.
     * application.dto에 두면 크로스 도메인·bff가 참조할 수 없어(위 규칙들) 재사용 시 빌드가 깨진다.
     */
    @Test
    fun `application_dto 패키지를 두지 않는다`() {
        noClasses()
            .should()
            .resideInAPackage("..application.dto..")
            .check(classes)
    }

    @Test
    fun `common 은 도메인을 참조하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..user..",
                "..place..",
                "..course..",
                "..mobile..",
            ).check(classes)
    }

    /**
     * BFF(mobile) 는 화면 조합을 위해 도메인에 의존하되, 공개 API(inbound 포트) 밖의 내부는 참조할 수 없다.
     * 실도메인 패키지는 `com.example.backend.<domain>..` 으로 앵커링한다 — 비앵커 `..<domain>..` 은
     * BFF 하위 도메인 패키지(`mobile.<domain>`)까지 매칭해, BFF 레이어 내부의 정상적인 상호 참조를 오탐한다.
     */
    @Test
    fun `BFF 는 도메인의 inbound 포트만 참조한다`() {
        for (target in listOf("user", "place", "course")) {
            val targetInternals =
                resideInAnyPackage(
                    "com.example.backend.$target.domain..",
                    "com.example.backend.$target.adapter..",
                    "com.example.backend.$target.application..",
                ).and(resideOutsideOfPackage("com.example.backend.$target.application.port.inbound.."))

            noClasses()
                .that()
                .resideInAPackage("..mobile..")
                .should()
                .dependOnClassesThat(targetInternals)
                .check(classes)
        }
    }

    /**
     * 크로스 도메인 격리 — 한 도메인은 다른 도메인의 내부(domain/adapter/service/dto/outbound 포트)에
     * 의존할 수 없고, 오직 상대 도메인의 application.port.inbound(공개 API)만 참조할 수 있다.
     * (BFF 패키지 mobile 은 이 규칙 밖이며 위의 전용 규칙이 담당한다.)
     *
     * 실도메인 패키지는 `com.example.backend.<domain>..` 으로 앵커링한다 — 비앵커 `..<domain>..` 은
     * BFF 하위 도메인 패키지(`mobile.<domain>`)까지 매칭해, BFF 레이어를 실도메인으로 오인한다.
     */
    @Test
    fun `도메인은 다른 도메인의 inbound 포트만 참조한다`() {
        val domains = listOf("user", "place", "course")
        for (dependent in domains) {
            for (target in domains) {
                if (dependent == target) continue
                // dependent 도메인은 target 도메인의 내부(도메인/어댑터/서비스/DTO/아웃바운드 포트)에 의존할 수 없다.
                // 허용되는 건 target.application.port.inbound 뿐이다.
                noClasses()
                    .that()
                    .resideInAPackage("com.example.backend.$dependent..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                        "com.example.backend.$target.domain..",
                        "com.example.backend.$target.adapter..",
                        "com.example.backend.$target.application.service..",
                        "com.example.backend.$target.application.port.outbound..",
                    ).check(classes)
            }
        }
    }

    /**
     * MSA 분리 대비 — 도메인 **코어(domain·application)** 는 다른 도메인을 **일절** 참조하지 않는다
     * (상대 도메인의 inbound 포트조차 금지). 크로스 도메인 접근은 **어댑터(ACL) 또는 bff** 에서만,
     * 상대 도메인의 `application.port.inbound` 로 한다(위 크로스 도메인 규칙이 어댑터 경로를 담당).
     * 즉 코어는 자기 도메인 + common 만 알고, 타 도메인 결합은 어댑터 경계에 격리된다 → 분리 시 어댑터만 교체.
     */
    @Test
    fun `도메인 코어는 다른 도메인을 참조하지 않는다`() {
        val domains = listOf("user", "place", "course")
        for (dependent in domains) {
            for (target in domains) {
                if (dependent == target) continue
                noClasses()
                    .that()
                    .resideInAnyPackage(
                        "com.example.backend.$dependent.domain..",
                        "com.example.backend.$dependent.application..",
                    ).should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.example.backend.$target..")
                    .check(classes)
            }
        }
    }
}
