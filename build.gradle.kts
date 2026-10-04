import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 루트는 빌드 산출물이 없다 — 서브모듈 커버리지를 하나의 JaCoCo 리포트로 합치는 역할만 한다.
// CI(ci.yml)는 `testCodeCoverageReport` 의 xml 을 읽는다. 모듈이 늘면 jacocoAggregation 에 추가한다.
plugins {
    base
    id("jacoco-report-aggregation")
    id("io.spring.dependency-management") // 집계가 app 의 런타임 클래스패스를 해석하므로 BOM 버전이 루트에도 필요하다
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    jacocoAggregation(project(":common"))
    jacocoAggregation(project(":app"))
}

reporting {
    reports {
        create<JacocoCoverageReport>("testCodeCoverageReport") {
            testSuiteName = "test"
            reportTask {
                reports.xml.required.set(true)
                reports.html.required.set(true)
            }
        }
    }
}
