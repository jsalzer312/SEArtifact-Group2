### ============================================= ###
### Shell script to fix issues related to study_1 ###
### ============================================= ###

#!/bin/bash

# File paths for the files to be addressed in the evaluations of CLIP, and BERT
CLIP_COMPONENT="study_1/clip/component_localization.py"
CLIP_SCREEN="study_1/clip/screen_localization.py"
BERT_SCREEN="study_1/sentence_bert/screen_and_component_localization.py"

# File paths with the correct pathing
OB_FILE_PATH="./study_1/dataset/real_data/ob/obs.json"
COMPONENT_IMAGES_FOLDER_PATH="./study_1/dataset/real_data/component_images"
SCREEN_FOLDER_PATH="./study_1/dataset/real_data/screen_images"
SCREEN_COMPONENTS_FOLDER_PATH="./study_1/dataset/real_data/screen_components/"

# Define the new device setting
NEW_DEVICE="cuda:0"
FULL_PATH=$(realpath .)

LUCENE_COMPONENT_PATH="study_1/lucene/src/main/java/ComponentLocalization.java"
LUCENE_SCREEN_PATH="study_1/lucene/src/main/java/ScreenLocalization.java"

SCREEN_COMPONENTS_FULL_FOLDER_PATH="$FULL_PATH/study_1/dataset/real_data/screen_components/"
OB_FULL_FILE_PATH="$FULL_PATH/study_1/dataset/real_data/ob/obs.json"

# Use sed to update the file
sed -i"" "s|^\([[:space:]]*\)screen_components_folder_path = .*|\1screen_components_folder_path = \"$SCREEN_COMPONENTS_FOLDER_PATH\"|" "$BERT_SCREEN"
sed -i"" "s|^\([[:space:]]*\)ob_file_path = .*|\1ob_file_path = \"$OB_FILE_PATH\"|" "$BERT_SCREEN"

sed -i"" "s|^\([[:space:]]*\)ob_file_path = .*|\1ob_file_path = \"$OB_FILE_PATH\"|" "$CLIP_COMPONENT"
sed -i"" "s|^\([[:space:]]*\)component_images_folder_path = .*|\1component_images_folder_path = \"$COMPONENT_IMAGES_FOLDER_PATH\"|" "$CLIP_COMPONENT"

sed -i"" "s|^\([[:space:]]*\)ob_file_path = .*|\1ob_file_path = \"$OB_FILE_PATH\"|" "$CLIP_SCREEN"
sed -i"" "s|^\([[:space:]]*\)screen_folder_path = .*|\1screen_folder_path = \"$SCREEN_FOLDER_PATH\"|" "$CLIP_SCREEN"

echo "\n \t\t~~~Updated the following files with the correct relative path~~~\n"
echo "$CLIP_COMPONENT"
echo "$CLIP_SCREEN"
echo "$BERT_SCREEN"


sed -i"" "s|^\([[:space:]]*\)String screenComponentsFolderPath = .*|\1String screenComponentsFolderPath = \"$SCREEN_COMPONENTS_FULL_FOLDER_PATH\";|" "$LUCENE_COMPONENT_PATH"
sed -i"" "s|^\([[:space:]]*\)String obFilePath = .*|\1String obFilePath = \"$OB_FULL_FILE_PATH\";|" "$LUCENE_COMPONENT_PATH"


sed -i"" "s|^\([[:space:]]*\)String screenComponentsFolderPath = .*|\1String screenComponentsFolderPath = \"$SCREEN_COMPONENTS_FULL_FOLDER_PATH\";|" "$LUCENE_SCREEN_PATH"
sed -i"" "s|^\([[:space:]]*\)String obFilePath = .*|\1String obFilePath = \"$OB_FULL_FILE_PATH\";|" "$LUCENE_SCREEN_PATH"


echo "\n \t\t~~~Updated the following files with the absolute file path~~~\n"
echo "$LUCENE_COMPONENT_PATH"
echo "$LUCENE_SCREEN_PATH"

sed -i"" "s|^\([[:space:]]*\)device = torch.device(\"cuda:[0-9]\")|\1device = torch.device(\"$NEW_DEVICE\")|" "$CLIP_COMPONENT"
sed -i"" "s|^\([[:space:]]*\)device = torch.device(\"cuda:[0-9]\")|\1device = torch.device(\"$NEW_DEVICE\")|" "$CLIP_SCREEN"

echo "\n \t\t~~~Updated the following files with the correct CUDA device setting~~~\n"
echo "$CLIP_COMPONENT"
echo "$CLIP_SCREEN"

# Usage: ./adjust_line.sh line_number "new_line_content"
FILE_PATH_DEPENDENCY="study_1/lucene/pom.xml"
FOLDER_PATH_DEPENDENCY="study_1/lucene/"
DEPENDENCIES="
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.11.0</version>
        </dependency>
        <dependency>
            <groupId>commons-cli</groupId>
            <artifactId>commons-cli</artifactId>
            <version>1.4</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-core</artifactId>
            <version>5.3.0</version>
        </dependency>
        <dependency>
            <groupId>edu.stanford.nlp</groupId>
            <artifactId>stanford-corenlp</artifactId>
            <version>3.8.0</version> <!-- Or the version you're using -->
        </dependency>
        <dependency>
            <groupId>edu.stanford.nlp</groupId>
            <artifactId>stanford-corenlp</artifactId>
            <version>3.8.0</version>
            <classifier>models</classifier>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>1.7.32</version>
        </dependency>
"

LINE_NUMBER=131

sed -i "${LINE_NUMBER}i\\
$(echo "$DEPENDENCIES" | sed 's/$/\\/')
" "$FILE_PATH_DEPENDENCY"

echo "\n \t\t~~~Added the missing dependencies to the following pom.xml $FILE_PATH_DEPENDENCY~~~\n"
echo "
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.11.0</version>
</dependency>
<dependency>
    <groupId>commons-cli</groupId>
    <artifactId>commons-cli</artifactId>
    <version>1.4</version>
</dependency>
<dependency>
    <groupId>org.apache.lucene</groupId>
    <artifactId>lucene-core</artifactId>
    <version>5.3.0</version>
</dependency>
<dependency>
    <groupId>edu.stanford.nlp</groupId>
    <artifactId>stanford-corenlp</artifactId>
    <version>3.8.0</version> <!-- Or the version you're using -->
</dependency>
<dependency>
    <groupId>edu.stanford.nlp</groupId>
    <artifactId>stanford-corenlp</artifactId>
    <version>3.8.0</version>
    <classifier>models</classifier>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.32</version>
</dependency>
"

cd $FOLDER_PATH_DEPENDENCY

echo "\n \t\t~~~Performing 'mvn clean install' for the following directory $FOLDER_PATH_DEPENDENCY~~~\n"

mvn clean install



