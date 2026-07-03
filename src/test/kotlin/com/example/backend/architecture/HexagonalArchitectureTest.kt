package com.example.backend.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * 헥사고날 + 도메인 분리 경계를 테스트로 강제한다(컴파일 대신 CI 로 위반 차단).
 *
 * 도메인이 하나(sample)뿐이라 크로스 도메인 규칙은 주석으로만 남겨두고,
 * member/order 등 두 번째 도메인이 생기면 활성화한다.
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
    fun `common 은 도메인을 참조하지 않는다`() {
        noClasses()
            .that()
            .resideInAPackage("..common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..sample..")
            .check(classes)
    }

    // TODO(두 번째 도메인 추가 시): 크로스 도메인 격리
    //  order.. 는 member.. 를 오직 order.adapter.outbound.member 에서,
    //  그것도 member.application.port.inbound 만 참조 가능하도록 규칙 추가.
}
