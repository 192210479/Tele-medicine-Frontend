import os

def refactor():
    base_dir = r"c:\Users\krish\AndroidStudioProjects\TMApp"
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file.endswith(".kt"):
                path = os.path.join(root, file)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                # Additional replacements
                new_content = new_content.replace("RetrofitClient.BASE_URL", "ApiClient.BASE_URL")
                
                if new_content != content:
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated {file}")

if __name__ == "__main__":
    refactor()
