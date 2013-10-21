package at.outdated.bitcoin.exchange.vircurex;

import at.outdated.bitcoin.exchange.api.ExchangeApiClient;
import at.outdated.bitcoin.exchange.api.Market;
import at.outdated.bitcoin.exchange.api.currency.Currency;
import at.outdated.bitcoin.exchange.api.market.AssetPair;
import at.outdated.bitcoin.exchange.api.market.transfer.TransferMethod;
import at.outdated.bitcoin.exchange.api.market.transfer.TransferType;

/**
 * Created by ebirn on 11.10.13.
 */
public class VircurexMarket extends Market {

    public VircurexMarket() {
        super("vircurex", "http://vircurex.com", "Vircurex", Currency.USD);

        withdrawals.put(Currency.BTC, new TransferMethod(Currency.BTC, TransferType.VIRTUAL, null));
        withdrawals.put(Currency.LTC, new TransferMethod(Currency.LTC, TransferType.VIRTUAL, null));
        withdrawals.put(Currency.NMC, new TransferMethod(Currency.NMC, TransferType.VIRTUAL, null));
        withdrawals.put(Currency.NVC, new TransferMethod(Currency.NVC, TransferType.VIRTUAL, null));

        deposits.put(Currency.BTC, new TransferMethod(Currency.BTC, TransferType.VIRTUAL, null));
        deposits.put(Currency.EUR, new TransferMethod(Currency.EUR, TransferType.BANK, null));
        deposits.put(Currency.USD, new TransferMethod(Currency.USD, TransferType.BANK, null));

        addAsset(Currency.BTC, Currency.LTC);
        addAsset(Currency.BTC, Currency.LTC);
        addAsset(Currency.BTC, Currency.NMC);
        addAsset(Currency.BTC, Currency.NVC);
        addAsset(Currency.LTC, Currency.NMC);
        addAsset(Currency.LTC, Currency.NVC);
        addAsset(Currency.NVC, Currency.NMC);
    }

    @Override
    public ExchangeApiClient getApiClient() {
        return new VircurexApiClient(this);
    }


}