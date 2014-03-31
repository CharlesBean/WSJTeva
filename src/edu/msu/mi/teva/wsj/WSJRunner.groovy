package edu.msu.mi.teva.wsj

import com.csvreader.CsvReader
import edu.mit.cci.adapters.csv.CsvBasedConversation
import edu.mit.cci.sna.Edge
import edu.mit.cci.teva.MemoryBasedRunner
import edu.mit.cci.teva.engine.Community
import edu.mit.cci.teva.engine.CommunityModel
import edu.mit.cci.teva.engine.ConversationChunk
import edu.mit.cci.teva.engine.TevaParameters
import edu.mit.cci.teva.model.Conversation
import edu.mit.cci.teva.model.DiscussionThread
import edu.mit.cci.teva.model.Post
import edu.mit.cci.teva.util.TevaUtils
import edu.mit.cci.text.windowing.Windowable
import edu.mit.cci.util.U

/**
 * Created by charlesbean on 3/25/14.
 */
class WSJRunner {

    Conversation conv

    public WSJRunner(File file) {
        conv = new CsvBasedConversation(["id", "replyTo", "start", "author", "text"] as String[], file.getName(), file.newInputStream(), '\t' as char, false) {
            public Date processDate(CsvBasedConversation.Column field, CsvReader reader) {
                new Date(((processString(field, reader) as float) * 1000f) as long)
            }
        }
    }

    def static populateAssignments(CommunityModel model, Map message_map, Map total_coverage_map) {
        model.communities.each { Community c ->
            c.assignments.each { ConversationChunk chunk ->
                chunk.messages.each { Windowable w ->
                    if (!message_map[w.id]) {
                        message_map[w.id] = new Object[model.communities.size()]
                        Arrays.fill(message_map[w.id], 0f)
                        total_coverage_map[w.id] = [edges: [] as Set<Edge>, total: 0f]

                    }
                    message_map[w.id][c.id as int] = chunk.coverage
                    if (chunk.coverage > 0) {
                        total_coverage_map[w.id]["edges"] += (chunk.edges)
                        total_coverage_map[w.id]["total"] = (total_coverage_map[w.id]["edges"].size() as float) * (chunk.coverage / (chunk.edges.size() as float))
                    }
                }
            }
        }
    }

    def static printSimpleOutput(CommunityModel model, Conversation conversation) {

        def message_map = [:]
        def total_coverage_map = [:]
        populateAssignments(model, message_map, total_coverage_map)
        def name = "smooth.${model.getCorpusName()}.q${model.getParameters().getFixedCliqueSize()}.x${model.getParameters().getMinimumLinkWeight()}.t${model.getParameters().getWordijIndirection()}.csv"

        new File(name).withWriter { Writer w ->
            w.println "time,${model.communities*.id.join(",")},totCov,diff,wDiff"
            conversation.allThreads.each { DiscussionThread t ->
                Post last = null;
                t.posts.each { Post p ->
                    def difference = 0
                    def weighteddiff = 0
                    if (last && message_map[last.postid] && message_map[p.postid]) {
                        def vals = (0..<model.communities.size()).collect {
                            def a = Math.abs(message_map[p.postid][it])
                            def b = Math.abs(message_map[last.postid][it])
                            [n: Math.abs(a - b), nw: Math.abs(a - b) * Math.max(a, b), d: Math.max(a, b)]
                        }

                        println "{$p.postid}. ${vals.findAll { it.d > 0 }}"

                        weighteddiff = 1 - (vals.nw.sum() / (vals.d.sum() ?: 1))
                        difference = 1 - (vals.n.sum() / (vals.d.sum() ?: 1))
                    } else {
                        println "{$p.postid}. No prior post"

                    }
                    last = p
                    w.println "${p.time.time},${message_map[p.postid] ? message_map[p.postid].join(",") : ""}, ${total_coverage_map[p.postid] ? total_coverage_map[p.postid]["total"] : ""},${difference},${weighteddiff}"
                }
            }
        }
    }

    public static void run(){

        File input = U.getAnyFile("WSJ File", ".", 0)

        Conversation c = new WSJRunner(input).conv

        TevaParameters tParams = new TevaParameters(System.getResourceAsStream("/wsj.teva.properties"))

        1.each { xof ->
            U.cleanDirectory(new File(tParams.getWorkingDirectory()));
            CommunityModel model = new MemoryBasedRunner(c, tParams, new WSJTevaFactory(tParams, c)).process();
            //TevaUtils.serialize(new File(tParams.getWorkingDirectory() + "/CommunityOuput." + c.getName() + "." + tParams.getFilenameIdentifier() + ".xml"), model, CommunityModel.class);
            //printSimpleOutput(model, c)
            println "hello"
        }
    }

    public static void main(String[] args){
        run()
    }
}
