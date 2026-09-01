package com.lensmatch.bridge;

import com.unity3d.player.UnityPlayerForGameActivity;

public class UnityBridge {

    public static void showFrame(String frameName) {
        UnityPlayerForGameActivity.UnitySendMessage(
                "FrameManager",
                "ShowFrame",
                frameName
        );
    }

    public static void clearFrame() {
        UnityPlayerForGameActivity.UnitySendMessage(
                "FrameManager",
                "ShowFrame",
                "none"
        );
    }
}
