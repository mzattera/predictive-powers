/*
 * Copyright 2023 Massimiliano "Maxi" Zattera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 */
package io.github.mzattera.autobot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

/**
 * Automatically create an Amazon Lex bot from a document or web page.
 * 
 * @author Massimiliano "Maxi" Zattera
 *
 */
public final class AutoBot {

	/**
	 * FOlder where to export the bot.
	 */
	private static final File OUTPUT_FOLDER = new File("D:\\LexBot");

	private AutoBot() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
//			Document input = Document.fromOfficeFile(new File("D:\\Using Data to Gain an Advantage in ESG Investing.docx"));
//			Document input = Document.fromURL("https://openai.com/dall-e-2/");

//			Document input = Document
//					.fromURL(new String[] { "https://www.open.ac.uk/courses/what-is-distance-learning/study",
//							"https://www.open.ac.uk/courses/what-is-distance-learning/tutors",
//							"https://www.open.ac.uk/courses/what-is-distance-learning/assessment" });

//			Document input = Document.fromURL("https://www.open.ac.uk/courses/careers/accountancy");

//			Document input = Document.fromOfficeFile(new File("D:\\OUData2.docx"));

			/*
			 * Document input =
			 * Document.fromString("BSc (Honours) Nursing - Learning outcomes\n" +
			 * "Educational aims\n" +
			 * "If you are already working as a healthcare support worker/healthcare assistant and would like to become a Registered Nurse, this unique practice-based qualification is for you. The aim of this course is for you to develop all the skills and proficiencies you need to be a Registered Nurse. The course enables you to stay in work while you study, provided you have the support of your employer. During the course, you will experience a range of alternative practice settings. On successful completion of the course you will be able to register as a Registered Nurse (Adult, Children and Young People, Learning Disabilities or Mental Health) with the Nursing and Midwifery Council (NMC). \n"
			 * + "\n" + "Key features of the course\n" + "\n" +
			 * "Balanced theory and practice delivered through practice-based and supportive distance learning\n"
			 * +
			 * "Develops the professional skills and knowledge to deliver high quality, safe and effective person/family-centred care for individuals across the life span with varied health needs in a range of care settings\n"
			 * +
			 * "Meets all the Nursing and Midwifery Council requirements for entry to the professional register as a Registered Nurse (Adult, Children and Young People, Learning Disabilities or Mental Health).\n"
			 * + "\n" + "Learning outcomes\n" + "Knowledge and understanding\n" +
			 * "On completion of this degree qualification, you will have knowledge and understanding of:\n"
			 * + "\n" +
			 * "a systematic and extensive knowledge and understanding of the contemporary context for health and social care, the range of settings and the principles of integrated, person-centred care.\n"
			 * +
			 * "an extensive knowledge and critical understanding of the biological, pharmacological, physical, socio-cultural, political, legal, ethical, organisational and psychological concepts and theories relevant to contemporary nursing practice.\n"
			 * +
			 * "an understanding of the principles of research, the evidence base for healthcare and nursing practice, and the ability to recognise the potential uncertainty, ambiguity and limits of knowledge.\n"
			 * + "Cognitive skills\n" +
			 * "On completion of this degree qualification, you will be able to:\n" + "\n" +
			 * "critique concepts and information from a wide range of sources, including current research, scholarly, and professional literature, and evaluate applicability for nursing and healthcare.\n"
			 * +
			 * "critically analyse risks in health and social care and evaluate strategies for improving safety and quality.\n"
			 * + "Practical and/or professional skills\n" +
			 * "On completion of this degree qualification, you will be able to:\n" +
			 * "demonstrate achievement of the Nursing and Midwifery Council proficiencies for registered nurses in the chosen field of practice (either Adult, Children and Young People, Learning Disabilities or Mental Health).\n"
			 * +
			 * "demonstrate resilience and acknowledge the impact and demands of professional nursing practice on your personal health and wellbeing, engaging in self-care and accessing support when required.\n"
			 * +
			 * "demonstrate independent learning skills and the ability to learn from feedback, reflecting on your own personal and professional development, and demonstrating a commitment to lifelong learning.\n"
			 * + "Key skills\n" +
			 * "On completion of this degree qualification, you will be able to demonstrate the following skills:\n"
			 * + "\n" +
			 * "Problem-solving, decision-making and critical thinking skills applied to nursing practice.\n"
			 * +
			 * "Ability to communicate effectively and manage relationships with service users, families, carers and health and social care colleagues, using a range of appropriate methods and applying emotional intelligence.\n"
			 * +
			 * "Literacy, digital literacy, technological literacy and numeracy skills required to ensure safe and effective nursing practice.\n"
			 * +
			 * "Effective skills in leadership, management, coordination, team-working and collaboration.\n"
			 * + "Teaching, learning and assessment methods\n" +
			 * "Our pre-registration nursing programme includes a range of teaching methods recognising that students have different preferred learning styles. During the programme you will learn directly from your experiences in practice and through knowledge and understanding acquired from specially prepared learning materials. You will be able to focus your learning on your field of practice although you will have the opportunity to reflect on practices across all four fields (Adult, Children and Young People, Learning Disabilities, and Mental Health).\n"
			 * + "\n" +
			 * "Each module guides your learning by providing underlying theory on key concepts and evidence-based knowledge to enable you to learn the necessary skills to practise nursing.\n"
			 * + "\n" +
			 * "While on programme, you will use an enquiry-based approach to your learning, and you will be encouraged to reflect on what you learn, carry out activities and participate in group learning. Peer learning will help avoid feelings of isolation that students can experience when studying via distance learning. For the level 2 and level 3 modules, enquiry-based learning uses scenarios/vignettes that reflect real life situations and the complexity of healthcare, people’s health needs across the lifespan and the range of care settings. Learning will be in small facilitated groups and you will develop skills to become an independent learner, who can identify your learning needs, seek out and appraise information, and apply your learning in practice.\n"
			 * + "\n" +
			 * "Information literacy and evidence-based practice are integral to your learning.\n"
			 * + "\n" +
			 * "To progress through the qualification, you will be required to integrate your theoretical learning (learning for practice) and your practice-based learning (learning in practice) in order to provide safe, effective, compassionate, person and family-centred care.\n"
			 * + "\n" +
			 * "As you progress, you will be expected to learn and practise increasing independence and will be encouraged to form ‘communities of learning’ with other students on the qualification. This will be promoted through face-to-face meetings where relevant and feasible, online discussion forum activities, online tutorials using Adobe Connect, telephone and email contact. A practice tutor/academic assessor, a practice-based supervisor and practice-based assessors will facilitate and support your practice learning in each setting where you gain experience.\n"
			 * + "\n" +
			 * "Support in practice will help you gain confidence and competence in order to deliver personalised, person/family-centred nursing care. Innovative educational technologies will be employed to maximise and capitalise on the excellent learning and teaching opportunities available. Vibrant and diverse learning materials and activities are brought together to bring an exciting and innovative approach to learning. The curriculum is flexible and student-centred, responsive to changing service requirements, and aims to produce nurses who can improve care experiences in a professional, caring and cost-effective manner.\n"
			 * + "\n" +
			 * "To recognise the value of nursing, students and staff will work collaboratively and creatively in the teaching and learning experience, to positively influence the nursing student’s personal and professional development. Enquiry Based Learning (EBL) is used to facilitate this development and to facilitate students acquiring requisite knowledge and skills. EBL is a student-centred teaching approach that motivates and engages students with direct decision making, applied analytical thinking and results in competent, reflective, autonomous practitioners, able to think critically, problem solve and respond in the ever-changing landscape of health and social care. This research-orientated approach will inspire students to learn for themselves in readiness for lifelong learning and leadership."
			 * );
			 */

//			Document input = Document.fromURL("https://en.wikipedia.org/wiki/Alan_Turing");

			Document input = Document.fromString(
					"Alan Mathison Turing OBE FRS (/ˈtjʊərɪŋ/; 23 June 1912 – 7 June 1954) was an English mathematician, computer scientist, logician, cryptanalyst, philosopher, and theoretical biologist.[6] Turing was highly influential in the development of theoretical computer science, providing a formalisation of the concepts of algorithm and computation with the Turing machine, which can be considered a model of a general-purpose computer.[7][8][9] He is widely considered to be the father of theoretical computer science and artificial intelligence.[10]\n"
							+ "\n"
							+ "Born in Maida Vale, London, Turing was raised in southern England. He graduated at King's College, Cambridge, with a degree in mathematics. Whilst he was a fellow at Cambridge, he published a proof demonstrating that some purely mathematical yes–no questions can never be answered by computation and defined a Turing machine, and went on to prove that the halting problem for Turing machines is undecidable. In 1938, he obtained his PhD from the Department of Mathematics at Princeton University. During the Second World War, Turing worked for the Government Code and Cypher School (GC&CS) at Bletchley Park, Britain's codebreaking centre that produced Ultra intelligence. For a time he led Hut 8, the section that was responsible for German naval cryptanalysis. Here, he devised a number of techniques for speeding the breaking of German ciphers, including improvements to the pre-war Polish bomba method, an electromechanical machine that could find settings for the Enigma machine. Turing played a crucial role in cracking intercepted coded messages that enabled the Allies to defeat the Axis powers in many crucial engagements, including the Battle of the Atlantic.[11][12]\n"
							+ "\n"
							+ "After the war, Turing worked at the National Physical Laboratory, where he designed the Automatic Computing Engine (ACE), one of the first designs for a stored-program computer. In 1948, Turing joined Max Newman's Computing Machine Laboratory, at the Victoria University of Manchester, where he helped develop the Manchester computers[13] and became interested in mathematical biology. He wrote a paper on the chemical basis of morphogenesis[1] and predicted oscillating chemical reactions such as the Belousov–Zhabotinsky reaction, first observed in the 1960s. Despite these accomplishments, Turing was never fully recognised in Britain during his lifetime because much of his work was covered by the Official Secrets Act.[14]\n"
							+ "\n"
							+ "Turing was prosecuted in 1952 for homosexual acts. He accepted hormone treatment with DES, a procedure commonly referred to as chemical castration, as an alternative to prison. Turing died on 7 June 1954, 16 days before his 42nd birthday, from cyanide poisoning. An inquest determined his death as a suicide, but it has been noted that the known evidence is also consistent with accidental poisoning. Following a public campaign in 2009, the British Prime Minister Gordon Brown made an official public apology on behalf of the British government for \"the appalling way [Turing] was treated\". Queen Elizabeth II granted a posthumous pardon in 2013. The term \"Alan Turing law\" is now used informally to refer to a 2017 law in the United Kingdom that retroactively pardoned men cautioned or convicted under historical legislation that outlawed homosexual acts.[15]\n"
							+ "\n"
							+ "Turing has an extensive legacy with statues of him and many things named after him, including an annual award for computer science innovations. He appears on the current Bank of England £50 note, which was released on 23 June 2021, to coincide with his birthday. A 2019 BBC series, as voted by the audience, named him the greatest person of the 20th century.\n"
							+ "\n" + "\n" + "Early life and education\n" + "Family\n"
							+ "Turing was born in Maida Vale, London, while his father, Julius Mathison Turing (1873–1947), was on leave from his position with the Indian Civil Service (ICS) of the British Raj government at Chatrapur, then in the Madras Presidency and presently in Odisha state, in India.[16][17] Turing's father was the son of a clergyman, the Rev. John Robert Turing, from a Scottish family of merchants that had been based in the Netherlands and included a baronet. Turing's mother, Julius's wife, was Ethel Sara Turing (née Stoney; 1881–1976), daughter of Edward Waller Stoney, chief engineer of the Madras Railways. The Stoneys were a Protestant Anglo-Irish gentry family from both County Tipperary and County Longford, while Ethel herself had spent much of her childhood in County Clare.[18] Julius and Ethel married on 1 October 1907 at Batholomew's church on Clyde Road, in Dublin.[19]\n"
							+ "\n"
							+ "Julius's work with the ICS brought the family to British India, where his grandfather had been a general in the Bengal Army. However, both Julius and Ethel wanted their children to be brought up in Britain, so they moved to Maida Vale,[20] London, where Alan Turing was born on 23 June 1912, as recorded by a blue plaque on the outside of the house of his birth,[21][22] later the Colonnade Hotel.[16][23] Turing had an elder brother, John (the father of Sir John Dermot Turing, 12th Baronet of the Turing baronets).[24]\n"
							+ "\n"
							+ "Turing's father's civil service commission was still active and during Turing's childhood years, his parents travelled between Hastings in the United Kingdom[25] and India, leaving their two sons to stay with a retired Army couple. At Hastings, Turing stayed at Baston Lodge, Upper Maze Hill, St Leonards-on-Sea, now marked with a blue plaque.[26] The plaque was unveiled on 23 June 2012, the centenary of Turing's birth.[27]\n"
							+ "\n"
							+ "Very early in life, Turing showed signs of the genius that he was later to display prominently.[28] His parents purchased a house in Guildford in 1927, and Turing lived there during school holidays. The location is also marked with a blue plaque.[29]\n"
							+ "\n" + "School\n"
							+ "Turing's parents enrolled him at St Michael's, a primary school at 20 Charles Road, St Leonards-on-Sea, from the age of six to nine. The headmistress recognised his talent, noting that she has \"...had clever boys and hardworking boys, but Alan is a genius\".[30]\n"
							+ "\n"
							+ "Between January 1922 and 1926, Turing was educated at Hazelhurst Preparatory School, an independent school in the village of Frant in Sussex (now East Sussex).[31] In 1926, at the age of 13, he went on to Sherborne School,[32] a boarding independent school in the market town of Sherborne in Dorset, where he boarded at Westcott House. The first day of term coincided with the 1926 General Strike, in Britain, but Turing was so determined to attend that he rode his bicycle unaccompanied 60 miles (97 km) from Southampton to Sherborne, stopping overnight at an inn.[33]\n"
							+ "\n"
							+ "Turing's natural inclination towards mathematics and science did not earn him respect from some of the teachers at Sherborne, whose definition of education placed more emphasis on the classics. His headmaster wrote to his parents: \"I hope he will not fall between two stools. If he is to stay at public school, he must aim at becoming educated. If he is to be solely a Scientific Specialist, he is wasting his time at a public school\".[34] Despite this, Turing continued to show remarkable ability in the studies he loved, solving advanced problems in 1927 without having studied even elementary calculus. In 1928, aged 16, Turing encountered Albert Einstein's work; not only did he grasp it, but it is possible that he managed to deduce Einstein's questioning of Newton's laws of motion from a text in which this was never made explicit.[35]\n"
							+ "\n" + "Christopher Morcom\n"
							+ "\n" + "University and work on computability\n"
							+ "After Sherborne, Turing studied as an undergraduate from 1931 to 1934 at King's College, Cambridge, where he was awarded first-class honours in mathematics. In 1935, at the age of 22, he was elected a Fellow of King's College on the strength of a dissertation in which he proved a version of the central limit theorem.[46] Unknown to Turing, this version of the theorem had already been proven, in 1922, by Jarl Waldemar Lindeberg. Despite this, the committee found Turing's methods original and so regarded the work worthy of consideration for the fellowship. Abram Besicovitch's report for the committee went so far as to say that if Turing's work had been published before Lindeberg's, it would have been \"an important event in the mathematical literature of that year\".[47][48][49]\n"
							+ "\n"
							+ "In 1936, Turing published his paper \"On Computable Numbers, with an Application to the Entscheidungsproblem\".[50] It was published in the Proceedings of the London Mathematical Society journal in two parts, the first on 30 November and the second on 23 December.[51] In this paper, Turing reformulated Kurt Gödel's 1931 results on the limits of proof and computation, replacing Gödel's universal arithmetic-based formal language with the formal and simple hypothetical devices that became known as Turing machines. The Entscheidungsproblem (decision problem) was originally posed by German mathematician David Hilbert in 1928. Turing proved that his \"universal computing machine\" would be capable of performing any conceivable mathematical computation if it were representable as an algorithm. He went on to prove that there was no solution to the decision problem by first showing that the halting problem for Turing machines is undecidable: it is not possible to decide algorithmically whether a Turing machine will ever halt. This paper has been called \"easily the most influential math paper in history\".[52]\n"
							+ "\n" + "\n"
							+ "King's College, Cambridge, where Turing was an undergraduate in 1931 and became a Fellow in 1935. The computer room is named after him.\n"
							+ "Although Turing's proof was published shortly after Alonzo Church's equivalent proof using his lambda calculus,[53] Turing's approach is considerably more accessible and intuitive than Church's.[54] It also included a notion of a 'Universal Machine' (now known as a universal Turing machine), with the idea that such a machine could perform the tasks of any other computation machine (as indeed could Church's lambda calculus). According to the Church–Turing thesis, Turing machines and the lambda calculus are capable of computing anything that is computable. John von Neumann acknowledged that the central concept of the modern computer was due to Turing's paper.[55] To this day, Turing machines are a central object of study in theory of computation.\n"
							+ "\n"
							+ "From September 1936 to July 1938, Turing spent most of his time studying under Church at Princeton University,[4] in the second year as a Jane Eliza Procter Visiting Fellow. In addition to his purely mathematical work, he studied cryptology and also built three of four stages of an electro-mechanical binary multiplier.[56] In June 1938, he obtained his PhD from the Department of Mathematics at Princeton;[57] his dissertation, Systems of Logic Based on Ordinals,[58][59] introduced the concept of ordinal logic and the notion of relative computing, in which Turing machines are augmented with so-called oracles, allowing the study of problems that cannot be solved by Turing machines. John von Neumann wanted to hire him as his postdoctoral assistant, but he went back to the United Kingdom.[60]\n"
							+ "\n" + "Career and research\n"
							+ "When Turing returned to Cambridge, he attended lectures given in 1939 by Ludwig Wittgenstein about the foundations of mathematics.[61] The lectures have been reconstructed verbatim, including interjections from Turing and other students, from students' notes.[62] Turing and Wittgenstein argued and disagreed, with Turing defending formalism and Wittgenstein propounding his view that mathematics does not discover any absolute truths, but rather invents them.[63]\n"
							+ "\n" + "Cryptanalysis\n"
							+ "During the Second World War, Turing was a leading participant in the breaking of German ciphers at Bletchley Park. The historian and wartime codebreaker Asa Briggs has said, \"You needed exceptional talent, you needed genius at Bletchley and Turing's was that genius.\"[64]\n"
							+ "\n"
							+ "From September 1938, Turing worked part-time with the Government Code and Cypher School (GC&CS), the British codebreaking organisation. He concentrated on cryptanalysis of the Enigma cipher machine used by Nazi Germany, together with Dilly Knox, a senior GC&CS codebreaker.[65] Soon after the July 1939 meeting near Warsaw at which the Polish Cipher Bureau gave the British and French details of the wiring of Enigma machine's rotors and their method of decrypting Enigma machine's messages, Turing and Knox developed a broader solution.[66] The Polish method relied on an insecure indicator procedure that the Germans were likely to change, which they in fact did in May 1940. Turing's approach was more general, using crib-based decryption for which he produced the functional specification of the bombe (an improvement on the Polish Bomba).[67]\n"
							+ "\n" + "\n"
							+ "Two cottages in the stable yard at Bletchley Park. Turing worked here in 1939 and 1940, before moving to Hut 8.\n"
							+ "On 4 September 1939, the day after the UK declared war on Germany, Turing reported to Bletchley Park, the wartime station of GC&CS.[68] Like all others who came to Bletchley, he was required to sign the Official Secrets Act, in which he agreed not to disclose anything about his work at Bletchley, with severe legal penalties for violating the Act.[69]\n"
							+ "\n"
							+ "Specifying the bombe was the first of five major cryptanalytical advances that Turing made during the war. The others were: deducing the indicator procedure used by the German navy; developing a statistical procedure dubbed Banburismus for making much more efficient use of the bombes; developing a procedure dubbed Turingery for working out the cam settings of the wheels of the Lorenz SZ 40/42 (Tunny) cipher machine and, towards the end of the war, the development of a portable secure voice scrambler at Hanslope Park that was codenamed Delilah.\n"
							+ "\n"
							+ "By using statistical techniques to optimise the trial of different possibilities in the code breaking process, Turing made an innovative contribution to the subject. He wrote two papers discussing mathematical approaches, titled The Applications of Probability to Cryptography[70] and Paper on Statistics of Repetitions,[71] which were of such value to GC&CS and its successor GCHQ that they were not released to the UK National Archives until April 2012, shortly before the centenary of his birth. A GCHQ mathematician, \"who identified himself only as Richard,\" said at the time that the fact that the contents had been restricted under the Official Secrets Act for some 70 years demonstrated their importance, and their relevance to post-war cryptanalysis:[72]\n"
							+ "[He] said the fact that the contents had been restricted \"shows what a tremendous importance it has in the foundations of our subject\". ... The papers detailed using \"mathematical analysis to try and determine which are the more likely settings so that they can be tried as quickly as possible\". ... Richard said that GCHQ had now \"squeezed the juice\" out of the two papers and was \"happy for them to be released into the public domain\".\n"
							+ "\n"
							+ "Turing had a reputation for eccentricity at Bletchley Park. He was known to his colleagues as \"Prof\" and his treatise on Enigma was known as the \"Prof's Book\".[73][74] According to historian Ronald Lewin, Jack Good, a cryptanalyst who worked with Turing, said of his colleague:\n"
							+ "\n"
							+ "In the first week of June each year he would get a bad attack of hay fever, and he would cycle to the office wearing a service gas mask to keep the pollen off. His bicycle had a fault: the chain would come off at regular intervals. Instead of having it mended he would count the number of times the pedals went round and would get off the bicycle in time to adjust the chain by hand. Another of his eccentricities is that he chained his mug to the radiator pipes to prevent it being stolen.[75]\n"
							+ "\n"
							+ "Peter Hilton recounted his experience working with Turing in Hut 8 in his \"Reminiscences of Bletchley Park\" from A Century of Mathematics in America:[76]\n"
							+ "\n"
							+ "It is a rare experience to meet an authentic genius. Those of us privileged to inhabit the world of scholarship are familiar with the intellectual stimulation furnished by talented colleagues. We can admire the ideas they share with us and are usually able to understand their source; we may even often believe that we ourselves could have created such concepts and originated such thoughts. However, the experience of sharing the intellectual life of a genius is entirely different; one realizes that one is in the presence of an intelligence, a sensibility of such profundity and originality that one is filled with wonder and excitement. Alan Turing was such a genius, and those, like myself, who had the astonishing and unexpected opportunity, created by the strange exigencies of the Second World War, to be able to count Turing as colleague and friend will never forget that experience, nor can we ever lose its immense benefit to us.\n"
							+ "\n"
							+ "Hilton echoed similar thoughts in the Nova PBS documentary Decoding Nazi Secrets.[77]\n"
							+ "\n"
							+ "While working at Bletchley, Turing, who was a talented long-distance runner, occasionally ran the 40 miles (64 km) to London when he was needed for meetings,[78] and he was capable of world-class marathon standards.[79][80] Turing tried out for the 1948 British Olympic team, but he was hampered by an injury. His tryout time for the marathon was only 11 minutes slower than British silver medallist Thomas Richards' Olympic race time of 2 hours 35 minutes. He was Walton Athletic Club's best runner, a fact discovered when he passed the group while running alone.[81][82][83] When asked why he ran so hard in training he replied:\n"
							+ "\n"
							+ "At the end of the war, a memo was sent to all those who had worked at Bletchley Park, reminding them that the code of silence dictated by the Official Secrets Act did not end with the war but would continue indefinitely.[69] Thus, even though Turing was appointed an Officer of the Order of the British Empire (OBE) in 1946 by King George VI for his wartime services, his work remained secret for many years.[87][88]\n"
							+ "\n" + "Bombe\n" + "Main article: Bombe\n"
							+ "Within weeks of arriving at Bletchley Park,[68] Turing had specified an electromechanical machine called the bombe, which could break Enigma more effectively than the Polish bomba kryptologiczna, from which its name was derived. The bombe, with an enhancement suggested by mathematician Gordon Welchman, became one of the primary tools, and the major automated one, used to attack Enigma-enciphered messages.[89]\n"
							+ "\n" + "\n"
							+ "The bombe searched for possible correct settings used for an Enigma message (i.e., rotor order, rotor settings and plugboard settings) using a suitable crib: a fragment of probable plaintext. For each possible setting of the rotors (which had on the order of 1019 states, or 1022 states for the four-rotor U-boat variant),[90] the bombe performed a chain of logical deductions based on the crib, implemented electromechanically.[91]\n"
							+ "\n"
							+ "The bombe detected when a contradiction had occurred and ruled out that setting, moving on to the next. Most of the possible settings would cause contradictions and be discarded, leaving only a few to be investigated in detail. A contradiction would occur when an enciphered letter would be turned back into the same plaintext letter, which was impossible with the Enigma. The first bombe was installed on 18 March 1940.[92]\n"
							+ "\n" + "Action This Day\n" + "Main article: Action This Day (memo)\n"
							+ "By late 1941, Turing and his fellow cryptanalysts Gordon Welchman, Hugh Alexander and Stuart Milner-Barry were frustrated. Building on the work of the Poles, they had set up a good working system for decrypting Enigma signals, but their limited staff and bombes meant they could not translate all the signals. In the summer, they had considerable success, and shipping losses had fallen to under 100,000 tons a month; however, they badly needed more resources to keep abreast of German adjustments. They had tried to get more people and fund more bombes through the proper channels, but had failed.[93]\n"
							+ "\n"
							+ "On 28 October they wrote directly to Winston Churchill explaining their difficulties, with Turing as the first named. They emphasised how small their need was compared with the vast expenditure of men and money by the forces and compared with the level of assistance they could offer to the forces.[93] As Andrew Hodges, biographer of Turing, later wrote, \"This letter had an electric effect.\"[94] Churchill wrote a memo to General Ismay, which read: \"ACTION THIS DAY. Make sure they have all they want on extreme priority and report to me that this has been done.\" On 18 November, the chief of the secret service reported that every possible measure was being taken.[94] The cryptographers at Bletchley Park did not know of the Prime Minister's response, but as Milner-Barry recalled, \"All that we did notice was that almost from that day the rough ways began miraculously to be made smooth.\"[95] More than two hundred bombes were in operation by the end of the war.[96]\n"
							+ "\n" + "\n"
							+ "Hut 8 and the naval Enigma\n"
							+ "Turing decided to tackle the particularly difficult problem of German naval Enigma \"because no one else was doing anything about it and I could have it to myself\".[98] In December 1939, Turing solved the essential part of the naval indicator system, which was more complex than the indicator systems used by the other services.[98][99]\n"
							+ "\n"
							+ "That same night, he also conceived of the idea of Banburismus, a sequential statistical technique (what Abraham Wald later called sequential analysis) to assist in breaking the naval Enigma, \"though I was not sure that it would work in practice, and was not, in fact, sure until some days had actually broken\".[98] For this, he invented a measure of weight of evidence that he called the ban. Banburismus could rule out certain sequences of the Enigma rotors, substantially reducing the time needed to test settings on the bombes.[100] Later this sequential process of accumulating sufficient weight of evidence using decibans (one tenth of a ban) was used in Cryptanalysis of the Lorenz cipher.[101]\n"
							+ "\n"
							+ "Turing travelled to the United States in November 1942[102] and worked with US Navy cryptanalysts on the naval Enigma and bombe construction in Washington; he also visited their Computing Machine Laboratory in Dayton, Ohio.\n"
							+ "\n" + "Turing's reaction to the American bombe design was far from enthusiastic:\n"
							+ "\n"
							+ "The American Bombe programme was to produce 336 Bombes, one for each wheel order. I used to smile inwardly at the conception of Bombe hut routine implied by this programme, but thought that no particular purpose would be served by pointing out that we would not really use them in that way. Their test (of commutators) can hardly be considered conclusive as they were not testing for the bounce with electronic stop finding devices. Nobody seems to be told about rods or offiziers or banburismus unless they are really going to do something about it.[103]\n"
							+ "\n"
							+ "During this trip, he also assisted at Bell Labs with the development of secure speech devices.[104] He returned to Bletchley Park in March 1943. During his absence, Hugh Alexander had officially assumed the position of head of Hut 8, although Alexander had been de facto head for some time (Turing having little interest in the day-to-day running of the section). Turing became a general consultant for cryptanalysis at Bletchley Park.[105]\n"
							+ "\n" + "Alexander wrote of Turing's contribution:\n" + "\n"
							+ "There should be no question in anyone's mind that Turing's work was the biggest factor in Hut 8's success. In the early days, he was the only cryptographer who thought the problem worth tackling and not only was he primarily responsible for the main theoretical work within the Hut, but he also shared with Welchman and Keen the chief credit for the invention of the bombe. It is always difficult to say that anyone is 'absolutely indispensable', but if anyone was indispensable to Hut 8, it was Turing. The pioneer's work always tends to be forgotten when experience and routine later make everything seem easy and many of us in Hut 8 felt that the magnitude of Turing's contribution was never fully realised by the outside world.[106]\n"
							+ "\n" + "Turingery\n" + "Main article: Turingery\n"
							+ "In July 1942, Turing devised a technique termed Turingery (or jokingly Turingismus)[107] for use against the Lorenz cipher messages produced by the Germans' new Geheimschreiber (secret writer) machine. This was a teleprinter rotor cipher attachment codenamed Tunny at Bletchley Park. Turingery was a method of wheel-breaking, i.e., a procedure for working out the cam settings of Tunny's wheels.[108] He also introduced the Tunny team to Tommy Flowers who, under the guidance of Max Newman, went on to build the Colossus computer, the world's first programmable digital electronic computer, which replaced a simpler prior machine (the Heath Robinson), and whose superior speed allowed the statistical decryption techniques to be applied usefully to the messages.[109] Some have mistakenly said that Turing was a key figure in the design of the Colossus computer. Turingery and the statistical approach of Banburismus undoubtedly fed into the thinking about cryptanalysis of the Lorenz cipher,[110][111] but he was not directly involved in the Colossus development.[112]\n"
							+ "\n" + "Delilah\n"
							+ "Following his work at Bell Labs in the US,[113] Turing pursued the idea of electronic enciphering of speech in the telephone system. In the latter part of the war, he moved to work for the Secret Service's Radio Security Service (later HMGCC) at Hanslope Park. At the park, he further developed his knowledge of electronics with the assistance of engineer Donald Bayley. Together they undertook the design and construction of a portable secure voice communications machine codenamed Delilah.[114] The machine was intended for different applications, but it lacked the capability for use with long-distance radio transmissions. In any case, Delilah was completed too late to be used during the war. Though the system worked fully, with Turing demonstrating it to officials by encrypting and decrypting a recording of a Winston Churchill speech, Delilah was not adopted for use.[115] Turing also consulted with Bell Labs on the development of SIGSALY, a secure voice system that was used in the later years of the war.\n"
							+ "\n" + "Early computers and the Turing test\n" + "\n"
							+ "Between 1945 and 1947, Turing lived in Hampton, London,[116] while he worked on the design of the ACE (Automatic Computing Engine) at the National Physical Laboratory (NPL). He presented a paper on 19 February 1946, which was the first detailed design of a stored-program computer.[117] Von Neumann's incomplete First Draft of a Report on the EDVAC had predated Turing's paper, but it was much less detailed and, according to John R. Womersley, Superintendent of the NPL Mathematics Division, it \"contains a number of ideas which are Dr. Turing's own\".[118]\n"
							+ "\n"
							+ "Although ACE was a feasible design, the effect of the Official Secrets Act surrounding the wartime work at Bletchley Park made it impossible for Turing to explain the basis of his analysis of how a computer installation involving human operators would work.[119] This led to delays in starting the project and he became disillusioned. In late 1947 he returned to Cambridge for a sabbatical year during which he produced a seminal work on Intelligent Machinery that was not published in his lifetime.[120] While he was at Cambridge, the Pilot ACE was being built in his absence. It executed its first program on 10 May 1950, and a number of later computers around the world owe much to it, including the English Electric DEUCE and the American Bendix G-15. The full version of Turing's ACE was not built until after his death.[121]\n"
							+ "\n"
							+ "In 1948, Turing was appointed reader in the Mathematics Department at the Victoria University of Manchester. A year later, he became deputy director of the Computing Machine Laboratory, where he worked on software for one of the earliest stored-program computers—the Manchester Mark 1. Turing wrote the first version of the Programmer's Manual for this machine, and was recruited by Ferranti as a consultant in the development of their commercialised machine, the Ferranti Mark 1. He continued to be paid consultancy fees by Ferranti until his death.[123] During this time, he continued to do more abstract work in mathematics,[124] and in \"Computing Machinery and Intelligence\" (Mind, October 1950), Turing addressed the problem of artificial intelligence, and proposed an experiment that became known as the Turing test, an attempt to define a standard for a machine to be called \"intelligent\". The idea was that a computer could be said to \"think\" if a human interrogator could not tell it apart, through conversation, from a human being.[125] In the paper, Turing suggested that rather than building a program to simulate the adult mind, it would be better to produce a simpler one to simulate a child's mind and then to subject it to a course of education. A reversed form of the Turing test is widely used on the Internet; the CAPTCHA test is intended to determine whether the user is a human or a computer.\n"
							+ "\n"
							+ "In 1948, Turing, working with his former undergraduate colleague, D.G. Champernowne, began writing a chess program for a computer that did not yet exist. By 1950, the program was completed and dubbed the Turochamp.[126] In 1952, he tried to implement it on a Ferranti Mark 1, but lacking enough power, the computer was unable to execute the program. Instead, Turing \"ran\" the program by flipping through the pages of the algorithm and carrying out its instructions on a chessboard, taking about half an hour per move. The game was recorded.[127] According to Garry Kasparov, Turing's program \"played a recognizable game of chess\".[128] The program lost to Turing's colleague Alick Glennie, although it is said that it won a game against Champernowne's wife, Isabel.[129]\n"
							+ "\n"
							+ "\n" + "Pattern formation and mathematical biology\n"
							+ "When Turing was 39 years old in 1951, he turned to mathematical biology, finally publishing his masterpiece \"The Chemical Basis of Morphogenesis\" in January 1952. He was interested in morphogenesis, the development of patterns and shapes in biological organisms. He suggested that a system of chemicals reacting with each other and diffusing across space, termed a reaction–diffusion system, could account for \"the main phenomena of morphogenesis\".[131] He used systems of partial differential equations to model catalytic chemical reactions. For example, if a catalyst A is required for a certain chemical reaction to take place, and if the reaction produced more of the catalyst A, then we say that the reaction is autocatalytic, and there is positive feedback that can be modelled by nonlinear differential equations. Turing discovered that patterns could be created if the chemical reaction not only produced catalyst A, but also produced an inhibitor B that slowed down the production of A. If A and B then diffused through the container at different rates, then you could have some regions where A dominated and some where B did. To calculate the extent of this, Turing would have needed a powerful computer, but these were not so freely available in 1951, so he had to use linear approximations to solve the equations by hand. These calculations gave the right qualitative results, and produced, for example, a uniform mixture that oddly enough had regularly spaced fixed red spots. The Russian biochemist Boris Belousov had performed experiments with similar results, but could not get his papers published because of the contemporary prejudice that any such thing violated the second law of thermodynamics. Belousov was not aware of Turing's paper in the Philosophical Transactions of the Royal Society.[132]\n"
							+ "\n"
							+ "Although published before the structure and role of DNA was understood, Turing's work on morphogenesis remains relevant today and is considered a seminal piece of work in mathematical biology.[133] One of the early applications of Turing's paper was the work by James Murray explaining spots and stripes on the fur of cats, large and small.[134][135][136] Further research in the area suggests that Turing's work can partially explain the growth of \"feathers, hair follicles, the branching pattern of lungs, and even the left-right asymmetry that puts the heart on the left side of the chest\".[137] In 2012, Sheth, et al. found that in mice, removal of Hox genes causes an increase in the number of digits without an increase in the overall size of the limb, suggesting that Hox genes control digit formation by tuning the wavelength of a Turing-type mechanism.[138] Later papers were not available until Collected Works of A. M. Turing was published in 1992.[139]\n"
							+ "\n" + "Personal life\n" + "Engagement\n"
							+ "In 1941, Turing proposed marriage to Hut 8 colleague Joan Clarke, a fellow mathematician and cryptanalyst, but their engagement was short-lived. After admitting his homosexuality to his fiancée, who was reportedly \"unfazed\" by the revelation, Turing decided that he could not go through with the marriage.[140]\n"
							);

			System.out.println("===[ Input Document ]==================================");
			System.out.println(input.text);

			input.addQnA();
			input.addQuestionVariations();
			exportBotV1(OUTPUT_FOLDER, "AutoBot", input);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final static String INTENT_JSON_V1 = "{" + "  \"metadata\": {" + "    \"schemaVersion\": \"1.0\","
			+ "    \"importType\": \"LEX\"," + "    \"importFormat\": \"JSON\"" + "  }," + "  \"resource\": {"
			+ "    \"name\": <NAME>," + "    \"version\": \"1\"," + "    \"intents\": <INTENTS>,"
			+ "    \"voiceId\": \"0\"," + "    \"childDirected\": false," + "    \"locale\": \"en-GB\","
			+ "    \"idleSessionTTLInSeconds\": 5940," + "    \"clarificationPrompt\": {" + "      \"messages\": ["
			+ "        {" + "          \"contentType\": \"PlainText\","
			+ "          \"content\": \"Sorry, can you please repeat that?\"" + "        }" + "      ],"
			+ "      \"maxAttempts\": 5" + "    }," + "    \"abortStatement\": {" + "      \"messages\": ["
			+ "        {" + "          \"contentType\": \"PlainText\","
			+ "          \"content\": \"Sorry, I could not understand. Goodbye.\"" + "        }" + "      ]" + "    },"
			+ "    \"detectSentiment\": false," + "    \"enableModelImprovements\": true" + "  }" + "}";

	/**
	 * From a document, exports a bot in LexJson V1 format. It assumes QnA pairs and
	 * questions variations have already been created for the document.
	 * 
	 * @param outFolder
	 * @param botName
	 * @throws IOException
	 */
	private static void exportBotV1(File outFolder, String botName, Document doc) throws IOException {

		Gson gson = new Gson();

		// List of created utterances, to make sure there are no duplicated utterances
		List<String> created = new ArrayList<>();

		// Make sure output folder exists and it is empty
		FileUtils.deleteDirectory(outFolder);
		outFolder.mkdir();

		// Intents
		@SuppressWarnings("unchecked")
		Map<String, Object>[] intents = new HashMap[doc.qnas.size()];

		Map<String, Object> fi = new HashMap<>();
		fi.put("type", "ReturnIntent");

		for (int i = 0; i < doc.qnas.size(); ++i) {

			Document.QnA qna = doc.qnas.get(i);

			// Intent name
			String name = qna.question.replaceAll("[^A-Za-z_]+", "_");
			if (name.length() > 90)
				name = name.substring(0, 90);

			Map<String, Object> intent = new HashMap<>();
			intent.put("name", name);
			intent.put("version", "1");

			intent.put("fulfillmentActivity", fi);

			String[] utterances = new String[qna.variations.size()];
			for (int j = 0; j < utterances.length; ++j)
				utterances[j] = qna.variations.get(j).replaceAll("\\?", "");

			intent.put("sampleUtterances", utterances);
			intent.put("slots", new Object[0]);

			Map<String, Object> tmp = new HashMap<>();
			tmp.put("groupNumber", 1);
			tmp.put("contentType", "PlainText");
			tmp.put("content", qna.answer);
			Map<String, Object> msg = new HashMap<>();
			msg.put("messages", new Object[] { tmp });
			intent.put("conclusionStatement", msg);

			intents[i] = intent;
		}

		String intentJSON = INTENT_JSON_V1;
		intentJSON = intentJSON.replace("<NAME>", gson.toJson(botName));
		intentJSON = intentJSON.replace("<INTENTS>", gson.toJson(intents));

		// Manifest
		write(new File(outFolder, botName + ".json"), intentJSON);
	}

	private final static String INTENT_JSON_V2 = "{\"name\":<NAME>,\"identifier\":\"XXXXXXXXXX\",\"description\":null,\"parentIntentSignature\":null,\"sampleUtterances\":<UTTERANCES>,\"intentConfirmationSetting\":null,\"intentClosingSetting\":null,\"initialResponseSetting\":{\"initialResponse\":null,\"conditional\":null,\"codeHook\":{\"isActive\":true,\"invocationLabel\":null,\"enableCodeHookInvocation\":true,\"postCodeHookSpecification\":{\"successNextStep\":{\"dialogAction\":{\"type\":\"FulfillIntent\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":{\"name\":null,\"slots\":null},\"sessionAttributes\":null},\"successResponse\":null,\"failureResponse\":null,\"failureNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":{\"name\":null,\"slots\":null},\"sessionAttributes\":null},\"failureConditional\":null,\"timeoutResponse\":null,\"timeoutNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":{\"name\":null,\"slots\":null},\"sessionAttributes\":null},\"timeoutConditional\":null,\"successConditional\":null}},\"nextStep\":{\"dialogAction\":{\"type\":\"InvokeDialogCodeHook\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":{\"name\":null,\"slots\":null},\"sessionAttributes\":null}},\"inputContexts\":null,\"outputContexts\":null,\"kendraConfiguration\":null,\"dialogCodeHook\":null,\"fulfillmentCodeHook\":{\"isActive\":true,\"fulfillmentUpdatesSpecification\":null,\"postFulfillmentStatusSpecification\":{\"successNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":{\"name\":null,\"slots\":null},\"sessionAttributes\":null},\"successResponse\":{\"allowInterrupt\":true,\"messageGroupsList\":[{\"message\":{\"imageResponseCard\":null,\"ssmlMessage\":null,\"customPayload\":null,\"plainTextMessage\":{\"value\":<ANSWER>}},\"variations\":null}]},\"failureResponse\":null,\"failureNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":{\"name\":null,\"slots\":null},\"sessionAttributes\":null},\"timeoutResponse\":null,\"timeoutNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":{\"name\":null,\"slots\":null},\"sessionAttributes\":null}},\"enabled\":false},\"slotPriorities\":[]}";

	/**
	 * From a document, exports a bot in LexJson V2 format. It assumes QnA pairs and
	 * questions variations have already been created for the document.
	 * 
	 * @param outFolder
	 * @param botName
	 * @throws IOException
	 */
	private static void exportBotV2(File outFolder, String botName, Document doc) throws IOException {

		Gson gson = new Gson();

		// List of created utterances, to make sure there are no duplicated utterances
		List<String> created = new ArrayList<>();

		// Make sure output folder exists and it is empty
		FileUtils.deleteDirectory(outFolder);
		outFolder.mkdir();

		// Manifest
		write(new File(outFolder, "Manifest.json"),
				"{\"metaData\":{\"schemaVersion\":\"1\",\"resourceType\":\"BOT\",\"fileFormat\":\"LexJson\"}}");

		// Bot data
		File botFolder = new File(outFolder, botName);
		botFolder.mkdir();
		write(new File(botFolder, "Bot.json"), "{\"name\":" + gson.toJson(botName)
				+ ",\"version\":\"DRAFT\",\"description\":null,\"identifier\":\"XXXXXXXXXX\",\"dataPrivacy\":{\"childDirected\":false},\"idleSessionTTLInSeconds\":300}");

		// Locale
		File localesFolder = new File(botFolder, "BotLocales");
		localesFolder.mkdir();
		File localeFolder = new File(localesFolder, "en_GB");
		localeFolder.mkdir();
		write(new File(localeFolder, "BotLocale.json"),
				"{\"name\":\"English (GB)\",\"identifier\":\"en_GB\",\"version\":null,\"description\":null,\"voiceSettings\":{\"engine\":\"neural\",\"voiceId\":\"Amy\"},\"nluConfidenceThreshold\":0.4}");

		// Intents
		File intentsFolder = new File(localeFolder, "Intents");
		intentsFolder.mkdir();
		int idx = 0;
		for (Document.QnA qna : doc.qnas) {

			// Intent name
			String name = qna.question.replaceAll("[^A-Za-z0-9\\-_]+", "_");
			if (name.length() > 90)
				name = name.substring(0, 90);
			name += (++idx);

			File intentFolder = new File(intentsFolder, name);
			intentFolder.mkdir();

			String intentJSON = INTENT_JSON_V2;
			intentJSON = intentJSON.replace("<NAME>", gson.toJson(name));

			List<String> utterances = qna.variations;
			List<Map<String, Object>> JUtterances = new ArrayList<>();
			for (String u : utterances) {

				// Avoid duplicates
				if (created.contains(u))
					continue;
				created.add(u);

				Map<String, Object> m = new HashMap<>();
				m.put("utterance", u);
				JUtterances.add(m);
			} // for each utterance in the intent
			intentJSON = intentJSON.replace("<UTTERANCES>", gson.toJson(JUtterances));

			intentJSON = intentJSON.replace("<ANSWER>", gson.toJson(qna.answer));

			write(new File(intentFolder, "Intent.json"), intentJSON);
		} // for each intent

		// Fallback intent
		File intentFolder = new File(intentsFolder, "FallbackIntent");
		intentFolder.mkdir();

		write(new File(intentFolder, "Intent.json"),
				"{\"name\":\"FallbackIntent\",\"identifier\":\"FALLBCKINT\",\"description\":\"Default intent when no other intent matches\",\"parentIntentSignature\":\"AMAZON.FallbackIntent\",\"sampleUtterances\":null,\"intentConfirmationSetting\":null,\"intentClosingSetting\":null,\"initialResponseSetting\":{\"initialResponse\":null,\"conditional\":null,\"codeHook\":{\"isActive\":true,\"invocationLabel\":null,\"enableCodeHookInvocation\":true,\"postCodeHookSpecification\":{\"successNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":null,\"sessionAttributes\":null},\"successResponse\":null,\"failureResponse\":null,\"failureNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":null,\"sessionAttributes\":null},\"failureConditional\":null,\"timeoutResponse\":null,\"timeoutNextStep\":{\"dialogAction\":{\"type\":\"EndConversation\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":null,\"sessionAttributes\":null},\"timeoutConditional\":null,\"successConditional\":null}},\"nextStep\":{\"dialogAction\":{\"type\":\"InvokeDialogCodeHook\",\"slotToElicit\":null,\"suppressNextMessage\":null},\"intent\":null,\"sessionAttributes\":null}},\"inputContexts\":null,\"outputContexts\":null,\"kendraConfiguration\":null,\"dialogCodeHook\":null,\"fulfillmentCodeHook\":null,\"slotPriorities\":[]}");
	}

	/**
	 * Write a string into a file with given encoding.
	 */
	private static void write(File fileName, String txt) throws IOException {

		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
			out.write(txt);
			out.flush();
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (Exception e) {
				}
		}
	}
}