//
//  SMBluetoothManagerFactory.h
//  StarIO_Extension
//
//  Created by Star Micronics* on 2017/**/**.
//  Copyright © 2017年 Star Micronics. All rights reserved.
//

#import <Foundation/Foundation.h>

#import <StarIO/SMBluetoothManager.h>

#import <StarIO_Extension/StarIoExt.h>

@interface SMBluetoothManagerFactory : NSObject

+ (SMBluetoothManager *)getManager:(NSString *)portName emulation:(StarIoExtEmulation)emulation;

@end
