//
//  ReactNativeStarPrinterBridge.m
//  ReactNativeStarPrinter
//
//  Created by Sébastien Vray on 03/04/2024.
//

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(ReactNativeStarPrinter, RCTEventEmitter)

RCT_EXTERN_METHOD(
                  searchPrinter: (RCTPromiseResolveBlock) resolve
                  rejecter: (RCTPromiseRejectBlock) rejecter
                  )

RCT_EXTERN_METHOD(
                  connect: (NSString) identifier
                  interface: (NSString) interface
                  resolver: (RCTPromiseResolveBlock) resolve
                  rejecter: (RCTPromiseRejectBlock) rejecter
                  )

RCT_EXTERN_METHOD(
                  getStatus: (RCTPromiseResolveBlock) resolve
                  rejecter: (RCTPromiseRejectBlock) rejecter
                  )

RCT_EXTERN_METHOD(
                  openCashDrawer: (RCTPromiseResolveBlock) resolve
                  rejecter: (RCTPromiseRejectBlock) rejecter
                  )

RCT_EXTERN_METHOD(
                  print: (id) commands
                  charset: NSString
                  resolver: (RCTPromiseResolveBlock) resolve
                  rejecter: (RCTPromiseRejectBlock) rejecter
                  )

RCT_EXTERN_METHOD(
                  showTextOnDisplay: (NSString) content
                  backLight: (BOOL) backLight
                  contrast: (NSInteger) contrast
                  cursorState: (NSString) cursorState
                  charset: (NSString) charset
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)rejecter
                  )

RCT_EXTERN_METHOD(
                  clearDisplay: (RCTPromiseResolveBlock) resolve
                  rejecter: (RCTPromiseRejectBlock) rejecter
                  )

RCT_EXTERN_METHOD(
                  disconnect: (RCTPromiseResolveBlock) resolve
                  rejecter: (RCTPromiseRejectBlock) rejecter
                  )

@end
