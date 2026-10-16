plugins {
    id("backend.library-conventions")
}

dependencies {
    api(project(":common"))
    implementation(project(":exposed-postgis"))
}
