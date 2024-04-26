require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

Pod::Spec.new do |s|
  s.name         = "ReactNativeStarPrinter"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['repository']['url']
  s.platforms     = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/wavyapp/react-native-star-prnt.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/**/*.{h,m,swift}"
  s.requires_arc = true
  s.private_header_files = "ios/libs/StarIO10.xcframework/**/*.h"

  # Use install_modules_dependencies helper to install the dependencies if React Native version >=0.71.0.
  # See https://github.com/facebook/react-native/blob/febf6b7f33fdb4904669f99d795eba4c0f95d7bf/scripts/cocoapods/new_architecture.rb#L79.
  if respond_to?(:install_modules_dependencies, true)
    install_modules_dependencies(s)
  else
    s.dependency "React-Core"

    # Don't install the dependencies when we run `pod install` in the old architecture.
    if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
      s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
      s.pod_target_xcconfig    = {
          "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
          "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1",
          "CLANG_CXX_LANGUAGE_STANDARD" => "c++17"
      }
      s.dependency "React-Codegen"
      s.dependency "RCT-Folly"
      s.dependency "RCTRequired"
      s.dependency "RCTTypeSafety"
      s.dependency "ReactCommon/turbomodule/core"
    end
  end

  s.pod_target_xcconfig = { 
    'EXCLUDED_ARCHS[sdk=iphoneos*]' => 'x86_64',
    'EXCLUDED_SOURCE_FILE_NAMES[sdk=iphoneos*]' => '$(SRCROOT)/../../node_modules/@wavyapp/react-native-star-prnt/ios/libs/StarIO10.xcframework/ios-arm64_x86_64-simulator/*.*',
    'FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]' => '$(SRCROOT)/** $(SRCROOT)/../../node_modules/@wavyapp/react-native-star-prnt/libs/ios $(SRCROOT)/../../node_modules/@wavyapp/react-native-star-prnt/ios/libs/StarIO10.xcframework/ios-arm64',
  }
  
  if ENV['USE_FRAMEWORKS']
    header_search_path = [
      '$(SRCROOT)/../../node_modules/react/** $(SRCROOT)/../../node_modules/react-native/**'
    ]

    exclude_source_file_name = [
      'libs/StarIO10.xcframework/ios-arm64_x86_64-simulator/StarIO10.framework/Headers/*.h libs/StarIO10.xcframework/ios-arm64_x86_64-simulator/StarIO10.framework/PrivateHeaders/*.h'
    ]

    s.pod_target_xcconfig  = {
      "HEADER_SEARCH_PATHS" => header_search_path.join(" "),
      "EXCLUDED_SOURCE_FILE_NAMES" => exclude_source_file_name.join(" ")
    }
  end
  s.vendored_frameworks = 'ios/libs/StarIO10.xcframework'
end
