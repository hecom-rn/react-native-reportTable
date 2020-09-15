import React from 'react';
import { Animated, Dimensions, PanResponder, ScrollView, View } from 'react-native';
import ReportTableView from './ReportTableView';

export default class ReportTableWrapper extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            headerHeight: 0,
        };

        this.showHeader = true;

        this.panResponder = PanResponder.create({
            onStartShouldSetPanResponder: () => true,
            onMoveShouldSetPanResponder: ()=> true,
            onPanResponderGrant: ()=>{},
            onPanResponderMove: (evt,gs)=>{
                if(gs.dy < 0 && this.showHeader){
                    this.scrollView &&
                    this.scrollView.scrollTo({ x: 0, y: -gs.dy , animated: true }, 1);
                }
            },
            onPanResponderRelease: (evt,gs)=>{}
        })
    }


    componentDidMount() {

    }


    render() {
        let { headerHeight } = this.state;
        const {headerView, size} = this.props;
        const data = this._toAndroidData();
        headerHeight = headerHeight > 20 ? headerHeight - 20 : headerHeight;
        return (
            <ScrollView
                ref={(ref) => (this.scrollView = ref)}
                style = {{flex: 1}}
                scrollEventThrottle={1}
                stickyHeaderIndices={[1]}
                onScroll = {(event)=>{{
                    if(event.nativeEvent.contentOffset.y >= headerHeight){
                        this.showHeader = false;
                    }else{
                        this.showHeader = true;
                    }
                }}}
            >
                <ScrollView
                    horizontal={true}
                    showsHorizontalScrollIndicator={false}
                    onLayout={(event) => {
                        const {
                            nativeEvent: {
                                layout: { height },
                            },
                        } = event;
                        this.setState({headerHeight: height})
                    }}
                >
                    {headerView && headerView()}
                </ScrollView>

                <ReportTableView
                    onScrollEnd={this.props.onScrollEnd}
                    onClickEvent={({nativeEvent: data}) => {
                        if (data) {
                            const {keyIndex, rowIndex, columnIndex, textColor} = data;
                            this.props.onClickEvent && this.props.onClickEvent({keyIndex, rowIndex, columnIndex});
                        }
                    }}
                    data={data}
                    style={{width: size.width, height: size.height + headerHeight }}
                    {...this.panResponder.panHandlers}
                />
            </ScrollView>
        )
    }

    _toAndroidData = () => {
        const {data, minWidth, minHeight, maxWidth, frozenColumns, frozenRows, frozenCount, frozenPoint} = this.props;
        const dataSource = {
            data: data,
            minWidth: minWidth,
            minHeight: minHeight,
            maxWidth: maxWidth,
            frozenRows: frozenRows,
            frozenColumns: frozenColumns,
            frozenPoint: frozenPoint,
            frozenCount: frozenCount,
        };
        const dataStr = JSON.stringify(dataSource);
        return dataStr;
    }

}

