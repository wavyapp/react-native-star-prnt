//
//  StarXpandCommandParserWrapper.h
//  StarIO10
//
//  Copyright 2021 Star Micronics Co., Ltd. All rights reserved.
//

#import <Foundation/Foundation.h>

#ifndef star_xpand_command_parser_wrapper
#define star_xpand_command_parser_wrapper

NS_ASSUME_NONNULL_BEGIN

@interface StarXpandCommandParserWrapper : NSObject

+ (nullable NSData *)parseWithCommand:(NSString *)command
                            emulation:(NSString *)emulation
                                model:(NSString *)model;

+ (nullable NSString *)replaceWithTemplate:(NSString *)templateStr
                                 fieldData:(NSString *)fieldData
                               errorTypeId:(int *)errorTypeId
                                 errorCode:(int *)errorCode
                            errorMessageId:(int *)errorMessageId;
@end

NS_ASSUME_NONNULL_END
#endif