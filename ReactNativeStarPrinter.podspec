require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "ReactNativeStarPrinter"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['repository']['url']
  s.platforms     = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/wavyapp/react-native-star-prnt.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/**/*.{h,m}"
  s.requires_arc = true

  s.dependency "React"
  s.vendored_frameworks = 'ios/Frameworks/StarIO.framework', 'ios/Frameworks/StarIO_Extension.framework'
end
