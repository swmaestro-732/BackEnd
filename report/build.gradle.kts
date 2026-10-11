plugins {
    id("backend.context-conventions")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":course"))
    implementation(project(":user"))
}
