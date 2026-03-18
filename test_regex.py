import re
with open('app/src/main/res/layout/activity_patient_dashboard.xml') as f:
    content = f.read()
print(re.findall(r'<LinearLayout[^>]*android:id="@+id/bottomNav"[^>]*>', content))
