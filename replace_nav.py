import os
import re

dir_path = "app/src/main/res/layout"

for filename in os.listdir(dir_path):
    if not filename.endswith(".xml") or filename == "layout_patient_bottom_nav.xml":
        continue
    filepath = os.path.join(dir_path, filename)
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    # ensure it is a patient layout
    if "navHome" in content and ("navHistory" in content or "navBook" in content):
        start_match = re.search(r'<LinearLayout[^>]*android:id="@+id/bottomNav"[^>]*>', content)
        if start_match:
            start_off = start_match.start()
            
            # Find the balanced closing </LinearLayout>
            depth = 0
            open_pattern = re.compile(r'<LinearLayout')
            close_pattern = re.compile(r'</LinearLayout>')
            
            # Find all open and close tags from start_off
            pos = start_off
            end_off = -1
            
            while True:
                next_open = open_pattern.search(content, pos)
                next_close = close_pattern.search(content, pos)
                
                if next_close is None:
                    break
                    
                if next_open and next_open.start() < next_close.start():
                    depth += 1
                    pos = next_open.end()
                else:
                    depth -= 1
                    pos = next_close.end()
                    if depth == 0:
                        end_off = pos
                        break
                        
            if end_off != -1:
                replacement = '    <include layout="@layout/layout_patient_bottom_nav"
        android:id="@+id/bottomNav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        app:layout_constraintBottom_toBottomOf="parent" />'
                new_content = content[:start_off] + replacement + content[end_off:]
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"? Replaced bottom nav in {filename}")
            else:
                print(f"? Failed to find balanced end in {filename}")
        else:
            print(f"?? No bottomNav block found in {filename}")
