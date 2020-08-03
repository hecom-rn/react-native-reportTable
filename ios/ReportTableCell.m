//
//  ReportTableCell.m
//  DoubleConversion
//
//  Created by ms on 2019/11/22.
//

#import "ReportTableCell.h"
#import "ReportTableView.h"

@implementation ReportTableCell

- (UILabel *)label {
    if (!_label) {
        _label = [UILabel new];
        _label.numberOfLines = 0;
    }
    return _label;
}


- (void)setTextPaddingHorizontal:(NSInteger)textPaddingHorizontal {
    CGFloat marginHor = textPaddingHorizontal;
    [self.contentView addConstraints:@[
                                [NSLayoutConstraint constraintWithItem:self.label attribute:NSLayoutAttributeLeft relatedBy:NSLayoutRelationEqual toItem:self.contentView attribute:NSLayoutAttributeLeft multiplier:1.0 constant:marginHor],

                                [NSLayoutConstraint constraintWithItem:self.label attribute:NSLayoutAttributeRight relatedBy:NSLayoutRelationEqual toItem:self.contentView attribute:NSLayoutAttributeRight multiplier:1.0 constant: self.isLocked ? - marginHor - 14 : - marginHor],

                                [NSLayoutConstraint constraintWithItem:self.label attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self.contentView attribute:NSLayoutAttributeCenterY multiplier:1.0 constant: 0],
                                ]
    ];
}

- (void)setIsLocked:(BOOL)isLocked {
    if (isLocked == true) {
        self.lockImageView.hidden = false;
    } else {
        if (_lockImageView) {
            [_lockImageView removeFromSuperview];
        }
    }
}

- (UIImageView *)lockImageView {
    if (!_lockImageView) {
        UIImage *image = [UIImage imageNamed:@"reportTableLock"];
        _lockImageView = [[UIImageView alloc] initWithImage: image];
        _lockImageView.translatesAutoresizingMaskIntoConstraints = false;
        _lockImageView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
        [self.contentView addSubview: _lockImageView];
        [self.contentView addConstraints:@[
                                    [NSLayoutConstraint constraintWithItem:_lockImageView attribute:NSLayoutAttributeRight relatedBy:NSLayoutRelationEqual toItem:self.contentView attribute:NSLayoutAttributeRight multiplier:1.0 constant: -10],
                                    [NSLayoutConstraint constraintWithItem:_lockImageView attribute:NSLayoutAttributeCenterY relatedBy:NSLayoutRelationEqual toItem:self.contentView attribute:NSLayoutAttributeCenterY multiplier:1.0 constant: 0],
                                    ]
        ];
    }
    return _lockImageView;
}



- (instancetype)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.label.frame = self.bounds;
        self.label.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
        [self.contentView addSubview:self.label];
        self.label.translatesAutoresizingMaskIntoConstraints = false;
        
    }
    return self;
}
@end
