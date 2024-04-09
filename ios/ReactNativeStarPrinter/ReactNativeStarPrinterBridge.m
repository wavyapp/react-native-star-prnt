//
//  ReactNativeStarPrinterBridge.m
//  ReactNativeStarPrinter
//
//  Created by Sébastien Vray on 03/04/2024.
//

#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(ReactNativeStarPrinter, RCTEventEmitter)

RCT_EXTERN_METHOD(
  portDiscovery: (NSString *)portType
  _: (RCTPromiseResolveBlock)resolve
  rejecter: (RCTPromiseRejectBlock)date
)

@end
