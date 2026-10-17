package app.studyforge.mobile;

import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** Apply system/keyboard safe areas to a layout, never to WebView's ignored padding. */
final class ScreenInsets {
    static void apply(View root,int extra){
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                view.setPadding(extra+safe.left,extra+safe.top,extra+safe.right,extra+safe.bottom);
                return WindowInsets.CONSUMED;
            }
            view.setPadding(extra+insets.getSystemWindowInsetLeft(),extra+insets.getSystemWindowInsetTop(),extra+insets.getSystemWindowInsetRight(),extra+insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        root.post(root::requestApplyInsets);
    }
}
