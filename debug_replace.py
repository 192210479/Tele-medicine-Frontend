import os
import re

dir_path = "app/src/main/res/layout"
for f in os.listdir(dir_path):
    if f == "activity_patient_dashboard.xml":
        with open(os.path.join(dir_path, f), "r", encoding="utf-8") as file:
            c = file.read()
            print("navHome:", "navHome" in c)
            print("navProfile:", "navProfile" in c)
            match = re.search(r'<LinearLayout[^>]*android:id="@+id/bottomNav"[^>]*>', c)
            print("match:", match)
