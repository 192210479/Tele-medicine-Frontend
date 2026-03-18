import os

dir_path = "app/src/main/res/layout"

for filename in os.listdir(dir_path):
    if not filename.endswith(".xml") or filename == "layout_patient_bottom_nav.xml":
        continue
    
    filepath = os.path.join(dir_path, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    # We want to replace the LinearLayout that contains the 4 bottom nav buttons.
    # We will find the index of `<LinearLayout android:id="@+id/bottomNav"`
    # or `<LinearLayout android:id="@+id/bottomNavAdmin"`
    # But wait, we only want patient layouts.
    if "navHome" in content and "navHistory" in content and "navProfile" in content:
        start_idx = content.find('<LinearLayout
        android:id="@+id/bottomNav"')
        if start_idx == -1:
            start_idx = content.find('<LinearLayout
            android:id="@+id/bottomNav"')
        if start_idx == -1:
            start_idx = content.find('<LinearLayout android:id="@+id/bottomNav"')
        if start_idx == -1:
            start_idx = content.find('<LinearLayout\n        android:id="@+id/bottomNav"')
            
        if start_idx != -1:
            # Find the closing </LinearLayout> for this block.
            # We count nested LinearLayouts.
            current_idx = start_idx
            depth = 0
            while current_idx < len(content):
                next_open = content.find("<LinearLayout", current_idx)
                next_close = content.find("</LinearLayout>", current_idx)
                
                if next_open != -1 and next_open < next_close:
                    depth += 1
                    current_idx = next_open + 13
                elif next_close != -1:
                    depth -= 1
                    current_idx = next_close + 15
                    if depth == 0:
                        end_idx = current_idx
                        break
                else: break

            if depth == 0:
                replacement = """<include layout="@layout/layout_patient_bottom_nav"
        android:id="@+id/bottomNav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        app:layout_constraintBottom_toBottomOf="parent" />"""
                new_content = content[:start_idx] + replacement + content[end_idx:]
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"Successfully processed {filename}")
            else:
                print(f"Failed to find end of block in {filename}")
        else:
            print(f"No bottomNav id found in {filename}")
