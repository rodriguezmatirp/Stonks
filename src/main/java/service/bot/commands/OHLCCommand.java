package service.bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import reactor.core.publisher.Mono;
import service.AlphaVantageHelper;
import service.MailHelper;
import symbol.SymbolData;
import symbol.plot.PlotTitles;
import symbol.plot.SymbolPlotter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OHLCCommand implements SlashCommand {
    static AlphaVantageHelper APIHelper = new AlphaVantageHelper();
    static MailHelper mailHelper = new MailHelper("zmechy2001@gmail.com", "gREENARROW_01");

    @Override
    public String getName() {
        return "ohlc";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        String symbolName = event.getOption("symbol")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get(); //This is warning us that we didn't check if its present, we can ignore this on required options

        String timePeriod = event.getOption("time-period")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        String mailTo = event.getOption("email")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get();

        String res = null;
        if (timePeriod == "daily"){
            res = APIHelper.dailyTimeSeries(symbolName);
        }
        else if(timePeriod == "monthly"){
            res = APIHelper.monthlyTimeSeries(symbolName);
        }
        else if(timePeriod == "weekly"){
            res = APIHelper.weeklyTimeSeries(symbolName);
        }
        else{
            res = APIHelper.latestTimeSeriesIntraday(symbolName, 5);
        }

        if (res == null){
            return  event.reply()
                    .withEphemeral(true)
                    .withContent("Some Error!");
        }

        SymbolData data = new SymbolData(res);
        PlotTitles titles = new PlotTitles("Main Title", "X - Axis Title", "Y - Axis Title");
        String imgPath = SymbolPlotter.buildOHLCChart(data, titles, 1600, 800);

        // message info
        String subject = "OHLC Chart for the specified period";
        StringBuffer body
                = new StringBuffer("<html>" + "Symbol : " + symbolName + "<br>");
        body.append("<img src=\"cid:ohlc_plot\" width=\"50%\" height=\"50%\" /><br>");
        body.append("</html>");

        // inline images
        Map<String, String> inlineImages = new HashMap<String, String>();
        inlineImages.put("ohlc_plot", imgPath);

        mailHelper.send(mailTo, subject, body.toString(), inlineImages);

        File f = new File(imgPath);
        f.delete();

        return event.reply().withContent("success");
    }
}