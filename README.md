# Argo
Runtime service discovery

*"To boldly go where hundreds have gone before!"*

### **What is DI2E Runtime Service Discovery?**

The Argo Runtime Service Discovery is a simple and robust protocol for the discovery and location of services on a wide-area network without the use of a central or federated registry.  The primary use-case is to communicate configuration information – such as IP Address and Port – to service consumers.  The technology and architecture behind Argo is not novel and has been in common use for decades (such as Bonjour and WS-Discovery).  Therefore, to go where hundreds have gone before.  There are only so many ways to do this.

Argo is extremely easy to use.  To have services your participate in the discovery ecosystem, simply install the Argo Responder according to the instructions and start the Responder.  This takes about 5-6 minutes.  There are no changes required to any code to advertise services.

### **Why another Service Discovery Protocol?**

Sure, there are a pile of them out there.  Why Argo?  Two main reason:

#### *Long-range protocol*
---------------------

Argo is meant to be a long-range or Wide Area protocol.  The existing protocols are almost exclusively designed to be used in the local-link network.  Meaning that the discovery probes they use are not meant to traverse and be routed out of the local router.  Argo was intented to be used in militrary networks to enable Network Centric Warfare.  With Argo probes, they can go as far as the multicast plan will allow - perhaps the entire extended network, including mobile ad-hoc mesh networks (MANETS).  However, this only really works when you adminstratively own the entire network.  The US Miltiary does, in fact, adminstratively own their networks.  How convenient for us.

#### *Security*
----------

Other mainstream protocols such as WS-Discovery and mDNS simply ignore security.  Argo allows a number of mutually allowable and strong security paradigms to be applied to the communcation packets.

### **Why is it called Argo?*

Argo is the name of both the ship Jason and the Argonaughts used as well as the name of a Ben Afflac movie.  The Argo discovery protocol has nothing to do with either of those things.  The name Argo was chosen for the two most important reasons an open source name is chosen: it sounded like a cool, short and catchy name and, perhaps more importantly, the domain name argo.ws was available.

The tagline, "to boldly go where hundreds have gone before", was added as an indicator that the technology underlying Argo was not new and was well founded to provide some assure that Argo was not some new voodoo, rocket science, deep magic.  And, it was a total rip off of a Neil deGrasse Tyson quote: "Boldly going where hundreds have gone before' does not make headlines."  However, a headline or two for Argo would be nice.
