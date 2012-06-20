package com.github.xuwei_k.ghscala

import org.specs2.Specification
import org.specs2.matcher.MatchResult

class Spec extends Specification{ def is =
  "repos" ! {
    forall(repos){case (user,_) =>
      forall(GhScala.repos(user)){ r =>
        println(r)
        (
          r must not beNull
        )and(
          forall(r.owner.productIterator){_ must not beNull}
        )
      }
    }
  } ^ "repo" ! {
    println(GhScala.repo(testUser,testRepo))
    success
  } ^ "refs" ! {
    check(GhScala.refs)
  } ^ "followers" ! {
    forall(repos){case (user,_) =>
      nullCheck(GhScala.followers(user))
    }
  } ^ "searchRepo" ! {
    nullCheck(GhScala.searchRepo(".g8"))
  } ^ "commits" ! {
    nullCheck(GhScala.commits(testUser,"ghscala",testSHA))
  } ^ "tree" ! {
    nullCheck(GhScala.trees(testUser,"ghscala",testSHA))
  } ^ "issue" ! {
    forallWithState(GhScala.issues)
  } ^ "an issue events" ! {
    val issues = List(("lift","framework",1254),("unfiltered","unfiltered",29))
    forall(issues){case (user,repo,num) =>
      forall(GhScala.issueEvents(user,repo,num)){ event =>
        nullCheck(event)
      }
    }
  } ^ "repository issue events" ! {
    check(GhScala.issueEvents)
  } ^ "search issues" ! {
    forallWithState(GhScala.searchIssues(_,_,"scala",_))
  } ^ "downloads" ! {
    val (repo,user) = ("eed3si9n","scalaxb")
    val list = GhScala.downloads(repo,user)

    def checkSingleDownloads() =
      forall(util.Random.shuffle(list.map{_.id}).take(list.size / 4)){
        id => nullCheck(GhScala.download(repo,user,id))
      }

    forall(list){nullCheck} and checkSingleDownloads()
  } ^ "forks" ! {
    check(GhScala.forks)
  } ^ "watchers" ! {
    check(GhScala.watchers)
  } ^ "watched" ! {
    forall(GhScala.watched(testUser)){nullCheck}
  } ^ "collaborators" ! {
    check(GhScala.collaborators)
  } ^ "comments" ! {
    check(GhScala.comments)
  } ^ "comments for single commit" ! {
    forall(GhScala.comments("scala","scala","989c0d0693e27d06d1f70524b66527d1ef12f5a2")){nullCheck}
  } ^ "readme" ! {
    forall(repos){case (user,repo) =>
      nullCheck(GhScala.readme(user,repo))
    }
  } ^ "readme ref param" ! {
    val `0.11.3` = GhScala.readme("harrah","xsbt","v0.11.3")
    val `0.11.2` = GhScala.readme("harrah","xsbt","v0.11.2")

    nullCheck(`0.11.2`) and nullCheck(`0.11.3`) and { `0.11.2` !== `0.11.3` }
  } ^ "pulls" ! {
    forallWithState(GhScala.pulls)
  } ^ "orgs" ! {
    forall(repos.map(_._1)){ user =>
      nullCheck(GhScala.orgs(user))
    }
  } ^ "org" ! {
    forall(testOrgs){ o =>
      nullCheck(GhScala.org(o))
    }
  } ^ end

  def forallWithState[A](f: (String,String,State) => List[A]) = {
    forall(repos){case (user,repo) =>
      val open   = f(user,repo,Open)
      val closed = f(user,repo,Closed)
      nullCheck(open) and nullCheck(closed) and {forall(open){closed must not contain(_)}}
    }
  }

  def check[A](func:(String,String) => List[A]) =
    forall(repos){case (user,repo) =>
      forall(func(user,repo)){nullCheck}
    }

  def nullCheck(obj:Any):MatchResult[Any] = {
    obj match{
      case coll:collection.GenTraversableOnce[_] =>
        (coll must not beNull) and forall(coll.toBuffer){nullCheck}
      case p:Product =>
        (p must not beNull) and forall(p.productIterator.toBuffer){nullCheck}
      case _ =>
        println(obj)
        obj must not beNull
    }
  }

  val testOrgs = List("scalajp","unfiltered","sbt","lift","dispatch")
  val repos = List(("etorreborre","specs2"),("dispatch","dispatch"),("scalaz","scalaz"),("unfiltered","unfiltered"))
  val testUser = "xuwei-k"
  val testRepo = "sbtend"
  val testSHA = "9cc84362e2487c4bb18e254445cf60a3fb7c5881"
}

