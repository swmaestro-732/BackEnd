plugins {
    id("backend.context-conventions")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":user-api"))
    implementation(project(":area"))
    implementation(project(":place"))
    implementation(project(":course"))
    implementation(project(":user"))
}
