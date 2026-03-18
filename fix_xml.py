import os
import re

dir_path = "app/src/main/res/layout"

for filename in os.listdir(dir_path):
    if not filename.endswith(".xml") or filename == "layout_patient_bottom_nav.xml":
        continue
    filepath = os.path.join(dir_path, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    # We replaced `<LinearLayout ... > ... </LinearLayout>` with `<include ... />`
    # However we left trailing `<LinearLayout ... > ... </LinearLayout>` siblings inside the parent, or a closing `</LinearLayout>` hanging
    # Let's see the structure of what went wrong:
    # 
    # original had:
    # <LinearLayout android:id="@+id/bottomNav">
    #    <LinearLayout navHome> ... </LinearLayout>
    #    <LinearLayout navBook> ... </LinearLayout>
    #    <LinearLayout navHistory> ... </LinearLayout>
    #    <LinearLayout navProfile> ... </LinearLayout>
    # </LinearLayout>
    #
    # New content has:
    # <include ... />
    # <LinearLayout navBook> ... </LinearLayout>
    # ...
    # </LinearLayout>
    #
    # Wait, my previous script:
    # start_idx found <LinearLayout android:id="@+id/bottomNav" (or similar),
    # but the recursive depth parse MIGHT have ended up matching the FIRST inner `<LinearLayout>` getting closed!
    # Yes! `<LinearLayout` depth went from 0 to 1, then the inner navHome opened so depth 2, then inner navHome closed so depth 1...
    # BUT wait! My logic initialized `depth = 0`, then the main tag opened, so depth = 1.
    # Ah! `start_off` was AT `<LinearLayout ...`.
    # At `pos = start_off`, `open_pattern.search` found the SAME `start_off`!
    # So `next_open` triggered, `depth += 1`.
    # Next, `navHome` opens, `depth += 1` => depth=2.
    # Next, `navHome` closes, `depth -= 1` => depth=1.
    # Next, `navBook` opens, `depth += 1` => depth=2.
    # Next, `navBook` closes, `depth -= 1` => depth=1.
    # Next, `navHistory` opens ...
    #
    # BUT WHAT IF `start_match` regex was `r'<LinearLayout[^>]*android:id="@+id/bottomNav"[^>]*>'`?
    # then `start_off` was exact. BUT the layout itself was:
    # ```
    # <LinearLayout id="bottomNav">
    #     <!-- Home -->
    #     <LinearLayout id="navHome">
    # ```
    # If the file didn't have a wrapper `id="bottomNav"` but instead my regex `start_match` completely missed the root because it didn't exist? No, the regex explicitly requires `id="@+id/bottomNav"`.
    # Wait, look at `activity_doctor_profile.xml` lines 283-306 from `view_file`.
    # It has:
    # ```
    # <!-- Bottom Navigation Mock -->
    # <include layout="@layout/layout_patient_bottom_nav"
    #     android:id="@+id/bottomNav" ... />
    #     
    #     <LinearLayout android:id="@+id/navBook" ...>
    # ...
    # </LinearLayout>
    # ```
    # 
    # This means the replacement ONLY REPLACED the `<LinearLayout id="navHome">` part???
    # No, it replaced `<LinearLayout id="bottomNav">` up to `</LinearLayout>` of `navHome` maybe?!
    # Look at the previous history:
    # The output of `activity_doctor_profile.xml` has `navBook`, `navHistory`, `navProfile` surviving.
    # Meaning my python script thought `depth` returned to 0 after `navHome` closed!
    # WHY?
    # `open_pattern = re.compile(r'<LinearLayout')`
    # `close_pattern = re.compile(r'</LinearLayout>')`
    # 
    # Ah!
    # `start_off` was the start of `<LinearLayout id="bottomNav"...`
    # `pos = start_off`
    # first iteration: `next_open` finds `<LinearLayout id="bottomNav"...` (because `pos = start_off`).
    # wait, `navHome` starts with `<LinearLayout id="navHome"`
    # IF `navHome` was commented out? No.
    # What if `content = content[:start_off] + replacement + content[end_off:]`
    # `end_off` resolved early!
    
    # We must fix ALL files that have this broken state. We can simply identify files with `<include layout="@layout/layout_patient_bottom_nav".*? />` 
    # AND still having `<LinearLayout android:id="@+id/navBook"` etc.
    if '<include layout="@layout/layout_patient_bottom_nav"' in content:
        # We need to wipe out the remaining dangling layout elements
        # Namely from after `<include ... />` until the final `</LinearLayout>` that was supposed to be the bottomNav wrapper.
        # How to find it?
        # The remaining part usually is:
        #         <LinearLayout android:id="@+id/navBook" ...
        # ...
        #     </LinearLayout>
        # </androidx.constraintlayout.widget.ConstraintLayout>
        
        # We can just run a regex to strip `<LinearLayout` chunks if they are purely navBook, navProfile, navHistory
        # Wait, if we just check for exactly those leftover blocks, we can remove them.
        
        # In activity_doctor_profile:
        # <include layout="@layout/layout_patient_bottom_nav" ... />
        #         <LinearLayout android:id="@+id/navBook" ... </LinearLayout>
        #         <LinearLayout android:id="@+id/navHistory" ... </LinearLayout>
        #         <LinearLayout android:id="@+id/navProfile" ... </LinearLayout>
        #     </LinearLayout>
        
        # A simpler way: we know `app:layout_constraintBottom_toBottomOf="parent" />` is where our `<include>` ends.
        
        # If we see `</LinearLayout>` right before `</androidx.constraintlayout.widget.ConstraintLayout>` or `</RelativeLayout>` maybe it's the dangling wrapper close tag.
        
        import xml.etree.ElementTree as ET
        # wait! xml parsing will fail because the XML is currently INVALID!
        pass
