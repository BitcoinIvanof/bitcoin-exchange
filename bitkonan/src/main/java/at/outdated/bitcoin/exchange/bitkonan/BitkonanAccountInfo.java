package at.outdated.bitcoin.exchange.bitkonan;

import at.outdated.bitcoin.exchange.api.account.AccountInfo;
import at.outdated.bitcoin.exchange.api.market.OrderType;
import at.outdated.bitcoin.exchange.api.market.fee.Fee;
import at.outdated.bitcoin.exchange.api.market.fee.SimplePercentageFee;

/**
 * Created with IntelliJ IDEA.
 * User: ebirn
 * Date: 26.05.13
 * Time: 23:44
 * To change this template use File | Settings | File Templates.
 */
public class BitkonanAccountInfo extends AccountInfo {

    @Override
    public Fee getTradeFee(OrderType trade) {

        return new SimplePercentageFee(0.0029);  //To change body of implemented methods use File | Settings | File Templates.
    }
}
