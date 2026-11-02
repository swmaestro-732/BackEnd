/** 공유 커널. 모든 모듈이 쓰는 타입만 두므로 내부까지 공개(OPEN)한다. */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.example.backend.common;
