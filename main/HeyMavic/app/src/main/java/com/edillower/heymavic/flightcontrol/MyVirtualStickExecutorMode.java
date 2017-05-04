package com.edillower.heymavic.flightcontrol;

/**
 * Virtual Stick Executor Mode
 * @author Eric Xu
 */

public enum MyVirtualStickExecutorMode {
    UNINITIALIZED,
    UP_WITHOUT_DIS,
    UP_DIS,
    DOWN_WITHOUT_DIS,
    DOWN_DIS,
    MOVE_WITHOUT_DIS,
    MOVE_DIS,
    FLY_TO,
    TURN,
    STOP
}