import os
import re

dir_path = "app/src/main/res/layout"

def cleanup():
    for filename in os.listdir(dir_path):
        if not filename.endswith(".xml") or filename == "layout_patient_bottom_nav.xml":
            continue
        filepath = os.path.join(dir_path, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()

        if '<include layout="@layout/layout_patient_bottom_nav"' in content:
            # We know the content continues with `<LinearLayout android:id="@+id/navBook"`
            # up to the last `</LinearLayout>` before `</androidx.constraintlayout.widget.ConstraintLayout>`, `</RelativeLayout>`, etc
            # Let's just do a regex that matches `navBook`, `navHistory`, `navProfile` blocks.
            
            navbook_idx = content.find('<LinearLayout android:id="@+id/navBook"')
            if navbook_idx == -1:
                # check if navHistory is first
                navbook_idx = content.find('<LinearLayout android:id="@+id/navHistory"')
                
            if navbook_idx != -1 and navbook_idx > content.find('<include layout="@layout/layout_patient_bottom_nav"'):
                # find the end tag. Since we're at the end of the file, let's just find the root closing tag
                root_tag_match = re.search(r'</[a-zA-Z.]+>$', content.strip())
                if root_tag_match:
                    root_tag = root_tag_match.group()
                    # cut out everything from navbook_idx up to the root tag
                    fixed_content = content[:navbook_idx].strip() + "\n\n" + root_tag + "\n"
                    with open(filepath, "w", encoding="utf-8") as f:
                        f.write(fixed_content)
                    print(f"Fixed {filename}")

cleanup()
