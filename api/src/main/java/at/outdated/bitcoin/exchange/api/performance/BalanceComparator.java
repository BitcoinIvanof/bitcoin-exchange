package at.outdated.bitcoin.exchange.api.performance;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: ebirn
 * Date: 25.06.13
 * Time: 14:07
 * To change this template use File | Settings | File Templates.
 */
public class BalanceComparator implements Comparator<Performance> {

    @Override
    public int compare(Performance o1, Performance o2) {
        return o1.getEndBalance().getValue().compareTo(o2.getEndBalance().getValue());
    }
}
