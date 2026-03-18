import os

def refactor():
    base_dir = r"c:\Users\krish\AndroidStudioProjects\TMApp\app\src\main\java\com\simats\tmapp"
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file.endswith(".kt"):
                path = os.path.join(root, file)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                # Imports
                new_content = new_content.replace("import com.simats.tmapp.api.RetrofitClient", "import com.simats.tmapp.api.ApiClient")
                new_content = new_content.replace("import com.simats.tmapp.api.ApiService", "import com.simats.tmapp.api.TelemedicineAPI")
                
                # Usage
                new_content = new_content.replace("RetrofitClient.apiService", "ApiClient.instance")
                
                if new_content != content:
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated {file}")

if __name__ == "__main__":
    refactor()
