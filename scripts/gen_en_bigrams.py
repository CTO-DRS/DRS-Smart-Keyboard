#!/usr/bin/env python3
"""Generates assets/dict/en_bigrams.txt — next-word statistics.

Compact hand-curated table of high-frequency English word pairs.
count = geometric decay by rank. The keyboard merges these counts with the
base dictionary to produce next-word suggestions.
"""

BIGRAMS = """
of the in the to the on the and the for the with the at the from the by the
as a as an as the than a than the into a into the onto a onto the over a over the
is a is the was a was the are the were the be the been the has a has the have a have the had a had the
do you do i did you can you could you would you should you will you won't you
i am i was i will i would i should i can i could i have i had i do i did i think i know i love i like i need i want i hope i feel i believe i guess i mean i see i hear i read i write i work i go i come i get i make i take i give i find i use i try i start i stop
you are you were you will you can you could you should you have you had you do you did you know you think you like you want you need you see you said you tell you ask
we are we were we will we can we should we have we need we want we think we know we go we come we make we take we do we see
they are they were they will they have they had they do they know they think they said they want they need
it is it was it will it has it had it does it seems it looks it feels it sounds it means
he is he was he will he has he had he does he said he knows he thinks he wants
she is she was she will she has she had she does she said she knows she thinks she wants
that is that was that would that will that sounds that looks that means
this is this was this will this seems this looks this means this way this time
there is there are there was there were there will there should
what is what was what do what does what are what about what if what time what day what happened
how are how is how do how does how did how much how many how long how far how often how about how come
when is when do when did when will when are
where is where do where did where are where were
why is why do why did why are why would
who is who was who do who did who are
good morning good night good evening good afternoon good luck good job good idea good news good time good day good work good point
great job great idea great work great news great time great day great place
thank you thanks for thank you
no problem no way no time no need no idea no more no one no matter no doubt
yes please yes sure yes of of course of them of us of my of your of his of her of our of their
one of one day one time one more one last one thing
some of some of some people some things some time some day some more some more
any of any one any time any way any more any other
all of all the all day all night all time all right all over all you all my all your
a lot a few a little a bit a lot a couple a while a moment a minute a second a day a week a year a time a place a person a friend
the same the other the best the most the first the last the next the new the old the good the great the right the wrong the only the way the time the day the world the people
in time in a in the in one in some in my in your in his in her in our in their in this in that in fact in case in order in front in addition in general in particular in return in short in total in touch in trouble
on time on the on a on my on your on his on her on our on their on this on that on top on hold on fire on purpose on average on track on demand
at home at work at school at least at most at first at last at all at times at once at night at noon at the at a
to be to do to go to have to make to take to get to see to know to say to tell to ask to come to think to work to play to help to talk to start to try to find to use to give to read to write to learn to teach to eat to drink to sleep to walk to run to drive to buy to sell to pay to send to call to meet to wait to look to listen to speak to understand
for a for the for me for you for him for her for us for them for this for that for all for some for more for example for instance for sure for real for now for today for tomorrow for good for free
with me with you with him with her with us with them with my with your with his with her with our with their with the with a with this with that with all with some
from the from a from me from you from us from them from my from your from this from that from now from here from there
about the about a about me about you about it about this about that about them about us about time about it
up to up in up on up with up for up all up and
out of out in out on out for out to out with out from
come to come in come on come here come back come up come down come out come with come from
go to go in go on go out go up go down go back go home go away go with go for go through
get to get in get on get up get out get off get back get home get ready get started get better get worse get done get it
look at look for look into look up look out look like look forward look around look back
take a take the take it take this take that take care take time take place take part take off take out take over take control
make a make the make it make this make that make sure make sense make time make money make room make way make up make out make friends
give me give you give it give up give in give out give away give back
tell me tell you tell him tell her tell them tell us tell it
let me let you let us let it let them let go let know
think about think of think it think that think so think twice
know about know what know how know who know why know when know where know that know it
want to want a want it want this want that want some want more
need to need a need it need some need more need help need time
like to like it like this like that like you like me
love you love it love this love them love to
time to time for time and time in time of time on
day of day to day and day in day out
year old year ago year in year at
back to back in back at back on back from back with
going to going on going in going out going home going back
want to going to have to has to had to used to able to supposed to about to
as well as much as many as long as soon as far as good as
so much so many so good so great so far so long so what so that so on
too much too many too good too bad too late too early too big too small
not a not the not to not for not in not on not at not with not all not even not just not only not really not sure
don't know don't want don't need don't like don't think don't worry don't care don't have don't you doesn't matter didn't know didn't see
can't believe can't wait can't stop can't help can't find can't see won't be won't go won't let
it's a it's the it's not it's just it's all it's time it's been it's good it's ok it's fine it's over
i'm sorry i'm sure i'm not i'm going i'm coming i'm here i'm back i'm fine i'm ok i'm good i'm tired i'm hungry i'm happy i'm ready i'm done
you're right you're wrong you're welcome you're the you're not you're so you're too you're going
we're here we're going we're not we're all we're back
they're here they're going they're not they're the
there's a there's no there's nothing there's something there's more
what's the what's your what's his what's her what's going what's happening
that's a that's the that's it that's all that's right that's good that's fine that's enough that's why
let's go let's start let's do let's try let's see let's talk let's meet let's play let's take
please let please do please go please come please send please send please help please tell please call please check please find
how much how many how long how far how often how good how bad how nice how great
same as same time same way same day same as
other than other day other people other side other hand
every day every time every one every year every week every month every night every morning
each other each one each time each day each side
first time first day first place first thing first of
last time last day last night last week last year last one last thing
next time next day next week next year next one next to
best friend best way best time best thing best day
new year new york new one new day new way new place new people new idea
old friend old man old woman old house old way old times
big deal big day big time big one big house small town small one small thing
long time long day long way long one long night
young man young woman young people high school high level low level low price
real life real time real world real friend true story true love right now right away right here right there
one thing one way one day one time one more one person two people two days two times three days four days five days six days seven days
i love you i miss you i need you i want you i like you i know you i see you i thank you i am here
miss you miss me miss it
talk to talk about talk with talk to
listen to listen for
write a write the write to write it
read a read the read it read this
send a send it send me send them
call me call you call him call her call them call us
check it check this check out check in
work on work in work at work with work for
play with play a play the play it
help me help you help them help with help out
sorry for sorry about sorry to
afraid of afraid to
proud of tired of tired from happy with happy about sad about angry with angry at mad at worried about excited about nervous about ready for good at bad at interested in
kind of kind of sort of type of lots of plenty of lots of a lot of
a and a but a or
and i and you and he and she and we and they and it and then and so and all and more
or not or more or less
if you if i if we if they if it if not if so
but i but you but he but she but we but they but it but not
because of because i because you because he because she because we because they because it because the
when i when you when he when she when we when they when it when the
while the while i while you
before the before i before you before we
after the after a after i after you after we after school after work
during the during a
people in people of people with people who
friends with friends and family and family of
work in work at work for work with
home in home at home with
day at night at night in night and
year in year of
way to way of way in
best in best of best for
much of much more much better
more than more of more and more in more for most of most people most of
less than less of
very good very nice very great very well very much very happy very important
really good really nice really great really well really want really need really like
just a just the just me just you just want just need just like just do
even if even more even better even the
still in still the still a still not
again and again to
always in always the always be never be never been never know never mind
anything else anything about anyone else anywhere else
something like something about something else something new
nothing more nothing else nobody knows everybody knows everyone knows everything is
my name my life my time my day my way my house my family my friends my work my school my book my phone my mother my father my brother my sister
your name your life your time your day your way your house your family your friends your work your book your phone your mother your father
his name her name our house their house its own my own your own
brother and sister and mom and dad and mother and father and
mister and doctor and
a the
to a to the in a in the on a on the at a at the for a for the with a with the from a from the by a by the of a of the as a as the
be a be the be in be on be at be to be for be with
have a have the have to have been has been had been will be would be could be should be can be may be might be must be
""".split("\n")

MORPH = {
    "friend": ["friends"], "family": ["families"], "day": ["days"],
    "year": ["years"], "week": ["weeks"], "month": ["months"],
    "time": ["times"], "way": ["ways"], "thing": ["things"],
    "person": ["people"], "child": ["children"], "man": ["men"],
    "woman": ["women"], "life": ["lives"], "book": ["books"],
    "word": ["words"], "work": ["works"], "place": ["places"],
    "good": ["better", "best"], "bad": ["worse", "worst"],
    "great": ["greater", "greatest"], "big": ["bigger", "biggest"],
    "small": ["smaller", "smallest"], "happy": ["happier", "happiest"],
}


def main():
    import os
    out_dir = os.path.join(os.path.dirname(__file__), "..", "DRSKeyboard", "app", "src", "main", "assets", "dict")
    os.makedirs(out_dir, exist_ok=True)
    rows = []
    rank = 0
    seen = set()
    for line in BIGRAMS:
        toks = line.split()
        i = 0
        while i + 1 < len(toks):
            a, b = toks[i], toks[i + 1]
            i += 1
            if not a.isalpha() or not b.isalpha() or "'" in a or "'" in b:
                continue
            key = (a.lower(), b.lower())
            if key in seen:
                continue
            seen.add(key)
            rows.append((key[0], key[1], max(4000 - rank * 7, 12)))
            rank += 1
    # seed morphological link pairs (go to -> goes to etc. handled by lemmatizer in app)
    for w, variants in MORPH.items():
        for v in variants:
            rows.append((w, v, 900))
    with open(os.path.join(out_dir, "en_bigrams.txt"), "w", encoding="utf-8") as f:
        for a, b, c in rows:
            f.write(f"{a}\t{b}\t{c}\n")
    print(f"en_bigrams.txt: {len(rows)} pairs")


if __name__ == "__main__":
    main()
