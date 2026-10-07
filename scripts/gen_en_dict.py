#!/usr/bin/env python3
"""Generates assets/dict/en_freq.txt — English wordlist in frequency-rank order.

Order below approximates Zipf rank for everyday typing (chat, email, notes).
Frequencies decay geometrically by rank. Morphological expansion adds safe
regular inflections so the trie can complete and correct inflected forms.
"""
import os

# ~3200 core words, roughly ordered most-frequent first.
CORE = """
the be to of and a in that have i it for not on with he as you do at this but his by from they we say her she or an will my one all would there their what so up out if about who get which go me when make can like time no just him know take people into year your good some could them see other than then now look only come its over think also back after use two how our work first well way even new want because any these give day most us is are was were been has had did having may should must might shall cant wont dont doesnt didnt isnt arent wasnt werent im ive ill youd youve youre theyre theyve lets
i me my myself we us our ours ourselves you your yours yourself yourselves he him his himself she her hers herself it its itself they them their theirs themselves what which who whom this these that those am is are was were be been being have has had having do does did doing will would shall should can could may might must
and but or nor so yet if then else when while because although though since unless until whenever wherever however therefore thus hence moreover furthermore meanwhile otherwise besides anyway indeed
time year day week month hour minute second moment period life world school state family student group country problem hand part place case week company system program question work government number night point home water room mother area money story fact lot right study book eye job word business issue side kind head house service friend father power hour game line end member law car city community name president team minute idea kid body information back parent face others level office door health person art war history party result change morning reason research girl guy moment air teacher force education foot boy age policy process music market sense nation plan college interest death experience effect use class control care field development role effort rate heart drug show leader light voice wife police mind price report decision son view relationship town road arm difference value building action model season society tax director position player record paper space ground form event official matter center couple site project project project figure street image phone data picture practice piece land product doctor wall patient worker news test movie north love support technology south board increase cast security bank culture east look feel story scene table plan attention fly review impact maybe beach design folk dog chance respect user staff yeah road key pressure charge bird tip bag bay bed bit block blow bone brush camp capital chain chair chest chip claim clay club coal coast coat code coffee column copy corner cream cup dark debt deck deep dish disk drama dream dress dust duty ease edge exit face fact fade fear fee feed fellow fence field fight film fine firm fish fit flag flat flood floor flow fold folk fond food foot ford fork form foul fox frog fun gap gift girl glory glove gold golf grace grade grain grand grass gray great green grid grill grip grow guard guess guest guide gun gut gym hair half hall hand harm hat hate health heat hedge help hero hill hint hit hold hole honey hood hook hope horn horse host hot hotel hour huge human humor ice idea inch index infant iron issue item ivory jacket jade jail jam jazz jet job join joke journey joy judge juice jump junk just keel key kidney king kiss kit knee knife knot lace lack lady lake lamb lamp land lane language lap lard large laser last late laugh lava lawn layer lead leaf lean leap learn leash least leather leave lecture left leg lemon lend length lens level lever liberty library license lid life lift light limb lime line link lion lip liquid list listen living load loan lock log lonely long look loop loose lord loss lot loud love luck lunch lung machine made mail main major make male mall man manager mango many map marble march mark market marriage mask mass master match material math meal mean meat meet melon member memory mention menu mercy merge merit metal method meter middle might milk mind mine minute mirror miss mistake mix model modern moment money monkey month moon moral morning mother motion motor mountain mouse mouth movie mud music mystery nail name nation native nature navy near neck need needle neighbor nerve nest net news nice niche night nine noble noise north nose note nothing novel now nurse nut oak object ocean odd offer office oil old one onion open opera orange orbit orchard order organ other ounce out outcome oven owl own ox pace pack page pain paint pair palace palm pan panda panel pants paper parade parent park part party pass past patch path patience pattern pause peace peach peak pen pencil people pepper period permit person pet phase phone photo phrase physics piano picture pie piece pier pig pile pilot pin pink pipe pitch pity place plan plane plant plastic plate play plaza pleasure plot plug plum pocket poem poet point poison polar police policy polish politics pond pool poor pop popular porch port portion position possible post pot potato pound power practice praise prayer prefer prepare present press pressure price pride priest primary prince print prison private prize problem process produce product profile program promise proof proper property proposal protect proud prove provide public pull pulse pump punch pupil purchase purple purpose purse push quality quantity quarter queen question queue quick quiet quilt quite quiz race radar radio rail rain raise rally range rank rapid rare rate rather ratio raw ray reach react read ready real really reason recall receipt receive recent record red reduce refer reform refuse regard region regular relax release relief religion rely remain remark remedy remember remote remove rent repair repeat replace reply report represent request require rescue research reserve resign resist respect respond rest result return reveal review reward rhythm rice rich ride ring rise risk river road roast robot rock role roll roof room root rope rose rough round route row rub rubber rule run rural rush rust sack sacred sad safe sail salad sale salt same sample sand save scale scan scare scene scent scheme school score scout scrap screen script sea search season seat second secret section security seed seek seem sell send sense separate series serious serve session set settle several shade shadow shake shall shape share sharp she sheep sheet shelf shell shield shift shine ship shirt shock shoe shoot shop shore short shot should shoulder shout show shut sick side sight sign signal silence silent silk silver similar simple since sincere single sink site situation size skill skin skirt sky slave sleep slice slide slight slip slope small smell smile smoke smooth snack snake soap soccer social society sock sofa soft soil solar soldier solid solution solve son song soon sort sound soup source south space spare speak special speed spell spend spice spider spike spin spirit split sport spot spray spread spring square squeeze stable stack staff stage stair stamp stand star start state statement station status stay steady steak steal steam steel steep stem step stick still sting stir stock stomach stone stop store storm story stove straight strain strait strange stream street strength stress stretch strict strike string strip stroke strong structure struggle student studio study stuff style subject submit substance subtle suburb succeed success such sudden suffer sugar suggest suit summer summit sun super supply support suppose sure surf surgeon surprise survive suspect sustain swallow swamp swap sweet swim swing switch symbol sympathy system table tactic tail take tale talk tall tank tape target task taste taxi tea teach team tear tech technique teeth telephone television tell temper temple tend tennis tent term terrible test text than thank that theater theme theory there thick thin thing think thirst this thought thousand thread threat thrill throat through throw thumb thunder ticket tide tiger tight time tin tiny tip tire title toast today toe together tomato tone tongue tonight tool tooth topic total touch tour tourist toward towel tower town toy trace track trade traffic trail train transfer transform transport trap travel tray treat tree trial tribe trick trip triumph trouble truck true trust truth tube tune tunnel turn turtle twelve twenty twice twin twist type typical ugly umbrella uncle under understand unit universe university unless until upper upset urban urge us use usual utility vacation vague valley value van variety vast vegetable vehicle venture verb version very vessel veteran viable victim victory video view village vinegar violence violin virus visa vision visit visual vitamin voice volume vote voyage wage wagon waist wait wake walk wall wand want war warm warn wash waste watch water wave way weak wealth weapon wear weather weave web wedding week weigh weight welcome well west wet whale wheat wheel where whether which while whisper white whole wide wife wild will win wind window wine wing winter wire wisdom wise wish witness wolf woman wonder wood wool word work world worry worth wound wrap wreck wrist write wrong yard year yellow yes yesterday yet young youth zero zone
morning afternoon evening tonight yesterday tomorrow today weekend birthday holiday vacation meeting dinner breakfast lunch supper snack drink coffee tea water milk juice beer wine bread rice pasta pizza burger sandwich cheese butter egg chicken beef fish shrimp salad soup dessert cake cookie candy chocolate sugar salt pepper oil vinegar onion garlic tomato potato carrot apple banana orange lemon lime grape strawberry blueberry peach cherry mango melon watermelon nut seed leaf root flower tree grass forest garden park river lake sea ocean beach mountain hill valley sky cloud rain snow wind storm thunder lightning sun moon star earth planet space universe animal bird cat dog horse cow pig sheep chicken duck fish bear wolf fox deer rabbit mouse rat squirrel lion tiger elephant giraffe zebra monkey snake frog turtle insect ant bee butterfly spider worm fly mosquito head face eye ear nose mouth lip tooth tongue hair neck shoulder arm elbow wrist hand finger nail leg knee foot toe skin heart blood bone brain stomach lung muscle body mind soul spirit
hello hi hey bye goodbye welcome thanks thank please sorry excuse pardon morning afternoon evening night good great fine okay ok yes no maybe sure right wrong true false love like hate enjoy prefer need want wish hope dream plan try start begin stop end finish continue keep hold catch throw give take bring send receive buy sell pay cost spend save earn lose win find look search seek help serve support assist guide lead follow chase run walk jump swim fly drive ride travel move stay wait rest sleep wake dream laugh cry smile frown talk speak say tell ask answer reply discuss share think know understand remember forget learn teach study read write listen hear watch see look observe notice believe doubt trust suspect agree disagree accept refuse offer suggest decide choose pick select prefer change turn open close shut lock unlock start begin stop finish enter exit arrive leave depart come go return stay remain sit stand lie lay sleep wake rise fall climb descend walk step run jog sprint crawl swim float dive fly land takeoff drive sail crash fix repair build create make destroy break mend sew knit cook bake boil fry roast grill steam taste smell touch feel hold grab grip pinch punch slap kick hit strike throw catch lift carry drag push pull drop send bring take fetch show display hide cover uncover wrap open fold bend straighten twist turn spin roll slide glide walk run jump hop skip dance sing play act perform entertain laugh smile cry weep sob shout whisper yell scream talk chat speak discuss argue debate agree disagree ask answer tell explain describe narrate count measure weigh compare calculate add subtract multiply divide
because since as although though while when whenever where wherever who whom whose which what why how if unless until before after during between among through across along against around behind below beneath beside besides beyond despite except inside outside near nearby off onto over under up down in out on at by for with without within throughout toward towards upon about above across after against along among around before behind below beneath beside between beyond but by down during except for from in inside into like near of off on onto out outside over past since through till to toward under until up upon with within without
email message phone call text chat meeting appointment schedule calendar deadline project task report presentation document file folder software hardware computer laptop tablet screen keyboard mouse printer camera speaker headphone internet website link password account login email address profile settings privacy security update download upload install uninstall app application program code develop design build test debug launch release version feature bug fix error crash performance improve optimize manage organize plan create edit delete remove share send receive save store backup sync cloud storage memory space speed slow fast connection network signal wifi data
monday tuesday wednesday thursday friday saturday sunday january february march april may june july august september october november december spring summer autumn fall winter
america american europe european asia asian africa african china chinese japan japanese korea korean india indian russia russian brazil egypt france french germany german italy italian spain spanish mexico canada australia london paris tokyo beijing delhi moscow berlin madrid rome cairo newyork
dr mr mrs ms prof drs ceo manager director president assistant employee worker boss colleague partner client customer user member leader chief officer agent expert specialist consultant advisor analyst engineer developer designer artist writer teacher professor student learner beginner professional amateur
please kindly thank thanks welcome sorry congrats congratulations happy sad excited angry tired sleepy hungry thirsty bored curious confused worried nervous scared afraid surprised shocked proud ashamed jealous glad delighted thrilled calm relaxed stressed anxious frustrated annoyed upset
omw brb idk imho btw fyi tbh lmk asap ty tvm np yw
""".split()

# Proper nouns to seed with lower frequency (keeps capital suggestions).
PROPER = """Monday Tuesday Wednesday Thursday Friday Saturday Sunday
January February March April May June July August September October November December
America American Europe European Asia Asian Africa African China Chinese Japan Japanese Korea Indian
Russia Brazil Egypt France Germany Italy Spain Mexico Canada Australia London Paris Tokyo Delhi Moscow
Berlin Madrid Rome Cairo Christmas Easter Ramadan Eid Allah
God Jesus Muhammad Ahmed Ali Omar Sara Maryam John Mary David Sarah Michael James Robert Linda
Google Apple Samsung Amazon Microsoft Facebook Twitter Instagram WhatsApp YouTube Internet
New York Washington Boston Chicago Houston Los Angeles San Diego Seattle
English Arabic French Spanish German Chinese Japanese
""".split()

# Safe inflection generator: only apply rules that are essentially always right.
def inflect(w: str):
    out = []
    if len(w) >= 3 and w.isalpha():
        if w.endswith(("s", "x", "z", "ch", "sh", "o")) and not w.endswith(("ss", "us", "is")):
            out.append(w + "es")
        elif w.endswith("y") and len(w) >= 4 and w[-2] not in "aeiou":
            out.append(w[:-1] + "ies")
            out.append(w[:-1] + "ied")
            out.append(w[:-1] + "ier")
            out.append(w[:-1] + "iest")
            out.append(w[:-1] + "ily")
        elif w.endswith("y") and len(w) >= 4:
            out.append(w + "s")
        else:
            out.append(w + "s")
        if w.endswith("e") and len(w) >= 4:
            out.append(w[:-1] + "ing")
            out.append(w[:-1] + "ed")
            if len(w) >= 5 and w[-2] not in "aeiou" and w[-3] in "aeiou":
                out.append(w + "r")
                out.append(w + "st")
                out.append(w + "ly")
        elif len(w) >= 3 and w[-1] not in "aeiouy" and w[-2] in "aeiou" and w[-3] not in "aeiou" and len(w) <= 6:
            # CVC doubling: sit->sitting (very safe subset)
            out.append(w + w[-1] + "ing")
            out.append(w + w[-1] + "ed")
        else:
            out.append(w + "ing")
            out.append(w + "ed")
        if w.endswith("y") and len(w) >= 4 and w[-2] in "aeiou":
            out.append(w + "ly")
        elif not w.endswith(("l", "y", "ly", "ing", "ed", "s")):
            out.append(w + "ly")
        out.append(w + "er")
        out.append(w + "est")
    return out


def main():
    out_dir = os.path.join(os.path.dirname(__file__), "..", "DRSKeyboard", "app", "src", "main", "assets", "dict")
    os.makedirs(out_dir, exist_ok=True)

    seen = set()
    rows = []
    rank_f = 0.0  # float rank → smooth Zipf decay

    def freq_at(r: float, damp: float = 1.0) -> int:
        return max(int(110000 * (0.99942 ** r) * damp), 35)

    def add(word, inflected=False):
        nonlocal rank_f
        w = word.strip().lower()
        if not w or not w.isalpha() or w in seen:
            return
        seen.add(w)
        rows.append((w, freq_at(rank_f, 0.35 if inflected else 1.0)))
        rank_f += 0.9 if inflected else 1.0

    for w in CORE:
        add(w)
        for inf in inflect(w):
            add(inf, inflected=True)

    # Proper nouns: stored capitalized (Dictionary keeps a display-form map).
    for w in PROPER:
        word = w.strip()
        if not word or not word.isalpha() or word in seen:
            continue
        seen.add(word.lower())
        rows.append((word, freq_at(rank_f, 0.55)))
        rank_f += 1.0

    rows.sort(key=lambda r: -r[1])
    with open(os.path.join(out_dir, "en_freq.txt"), "w", encoding="utf-8") as f:
        for w, freq in rows:
            f.write(f"{w}\t{freq}\n")
    print(f"en_freq.txt: {len(rows)} words, top={rows[0][1]}, #1000={rows[999][1]}, last={rows[-1][1]}")


if __name__ == "__main__":
    main()
