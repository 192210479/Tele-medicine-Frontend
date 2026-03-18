import os, re

dir_path = "app/src/main/res/layout"

# Regex for finding a `<LinearLayout` that contains `android:id="@+id/navHistory"` inside it 
# and replacing the entire outer `<LinearLayout>` ... `</LinearLayout>`.
#
# But wait, it might contain `navHome`, `navBook`, etc. We only want to replace the outer LinearLayout that encapsulates `navHome` and `navProfile` and `navHistory`.
# We know the outer layout has `navHome` AND ends exactly matching `bottomNav` in most files.

for filename in os.listdir(dir_path):
    if not filename.endswith(".xml") or filename == "layout_patient_bottom_nav.xml":
        continue
        
    filepath = os.path.join(dir_path, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
        
    if "navHome" in content and "navHistory" in content and "navProfile" in content:
        # Find the start of the <LinearLayout that is the parent of navHome.
        # This wrapper usually has android:id="@+id/bottomNav" or simply sits at the very end of the main tag.
        
        # We can find where <LinearLayout id="...navHome" starts.
        nav_home_idx = content.find('"@+id/navHome"')
        if nav_home_idx == -1: continue
        
        # Search backwards for the parent <LinearLayout
        target_start = -1
        # It's an id of bottom nav or just a wrapper.
        # Let's search back for "<LinearLayout"
        cursor = nav_home_idx
        while cursor > 0:
            cursor = content.rfind("<LinearLayout", 0, cursor)
            if cursor == -1: break
            # Does this tag have id bottomNav? Or does it not have an id?
            start_tag = content[cursor:content.find(">", cursor)]
            if '"@+id/bottomNav"' in start_tag or 'android:layout_width="match_parent"' in start_tag:
                target_start = cursor
                # Check if this tag has a closing tag that surrounds navHome
                # (Simple check: is it the first LinearLayout before navHome? We want the root of the nav, which is usually one level up.)
                
        if target_start != -1:
            # Let's do a more structured parse by matching `<LinearLayout` to `</LinearLayout>` exactly.
            pass

# Since Python parsing may be complex, I'll use a precise regex that looks for typical attributes:
import re
for filename in os.listdir(dir_path):
    if not filename.endswith(".xml") or filename == "layout_patient_bottom_nav.xml":
        continue
    filepath = os.path.join(dir_path, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
        
    if "navHome" in content and "navHistory" in content and "navProfile" in content:
        # The parent LinearLayout usually has android:id="@+id/bottomNav"
        pattern = re.compile(r'<LinearLayout[^>]*android:id="@+id/bottomNav"[^>]*>.*?</LinearLayout>', re.DOTALL)
        if pattern.search(content):
            print(f"Matched {filename}")
            new_content = pattern.sub('<include layout="@layout/layout_patient_bottom_nav" android:id="@+id/bottomNav" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_alignParentBottom="true" app:layout_constraintBottom_toBottomOf="parent" />', content)
            if new_content != content:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"   -> Replaced in {filename}")
        else:
            print(f"No bottomNav id in {filename}")
