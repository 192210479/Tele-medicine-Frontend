import os

directory = r'c:\Users\krish\AndroidStudioProjects\TMApp\app\src\main\res\layout'
files = [
    'activity_login.xml', 'activity_reset_password.xml', 'activity_check_email.xml', 
    'activity_create_account_role.xml', 'activity_create_account_details.xml', 
    'activity_create_doctor_details.xml', 'activity_patient_dashboard.xml', 
    'activity_emergency_help.xml', 'activity_select_doctor.xml', 
    'activity_onboarding1.xml', 'activity_onboarding2.xml', 
    'activity_onboarding3.xml', 'activity_auth_welcome.xml'
]

for f in files:
    path = os.path.join(directory, f)
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as file:
            content = file.read()
        
        content = content.replace('android:layout_marginTop="60dp"', 'android:layout_marginTop="40dp"')
        content = content.replace('android:layout_marginTop="48dp"', 'android:layout_marginTop="32dp"')
        content = content.replace('android:layout_marginTop="32dp"', 'android:layout_marginTop="24dp"')
        content = content.replace('android:layout_marginTop="24dp"', 'android:layout_marginTop="16dp"')
        content = content.replace('android:layout_marginTop="16dp"', 'android:layout_marginTop="10dp"')
        
        content = content.replace('padding="24dp"', 'padding="16dp"')
        content = content.replace('paddingHorizontal="24dp"', 'paddingHorizontal="16dp"')
        content = content.replace('paddingTop="24dp"', 'paddingTop="16dp"')
        content = content.replace('paddingBottom="24dp"', 'paddingBottom="16dp"')
        
        with open(path, 'w', encoding='utf-8') as file:
            file.write(content)

print("done")
