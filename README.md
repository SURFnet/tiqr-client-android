# tiqr-client-android

tiqr client for Android

# Create a store build

To create a release build intented to be released on the play store, follow these steps:
1. Bump the app version
2. Set enviroment variables:

Environment variables for the Android SDK
```bash
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH
export PATH=$ANDROID_HOME/tools:$PATH
export PATH=$ANDROID_HOME/build-tools/29.0.3/:$PATH
```
*Note: change `build-tools` to the (latest) version installed*


Environment variables for Fastlane
```bash
export TIQR_FASTLANE_STORE_FILE=~/.signing/Tiqr.keystore
export TIQR_FASTLANE_STORE_PASS=<your_keystore_pass>
export TIQR_FASTLANE_KEY_ALIAS=<your_key_alias>
export TIQR_FASTLANE_KEY_PASS=<your_key_pass>
```
*Note: adapt above to your own data*

3. Run the Fastlane command:
```bash
cd fastlane
fastlane android storebuild
```
*If you don't have fastlane, install it via the fastlane instructions.*

4. The apk and mapping files are now available in the `distribution` subdir. 
