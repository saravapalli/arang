---
title: Arang — Ingestion Framework (P3)
type: PRD
artifact_id: ARANG-PRD-P3
parent: ARANG-INIT-001
status: placeholder
version: 0.0
related_docs:
  - docs/prd.md (initiative overview)
tags: [ingestion, connectors, pipeline, scraping, api]
---

# Arang — Ingestion Framework (P3) — PLACEHOLDER

> Not yet specified. Lowest priority. **Note from the spec-reviewer audit:** this feature
> is itself initiative-sized (7 connector shapes + de-duplication + an inbound Push API).
> When we reach it, it should likely become its own sub-initiative with child PRDs per
> connector shape, NOT a single PRD — otherwise it breaks the right-sizing caps.

**Intended scope (summary; see initiative overview §5 P3):** a pluggable connector
framework (inbound push API, outbound pull API, structured web, unstructured web/JS
calendars, documents/PDF, images, compliance-gated messaging/social) feeding one shared
acquire→normalize→extract→dedupe→store pipeline producing `event` records with provenance.

**Hard gates before building any connector:** per-source legal/ToS review (blocking for
messaging/social); de-duplication strategy; inbound API auth/rate-limiting. The cheapest,
safest first connector is the **inbound Push API**.


---

## Appendix A — Seed source list for the first five towns (research, 2026-11-01)

> **Status of this appendix: CANDIDATE list, not approved sources.** It is input for the source
> registry (see the registry fields below). Nothing here has been fetched by Arangly, and no source
> has passed the per-source legal/terms review that this PRD requires before any connector is built.
>
> **What was checked:** each URL below appeared in a web search result on 2026-11-01. The search
> snippets showed that the page lists events or programs.
> **What was NOT checked:** (1) whether a page offers an ICS or RSS feed — an attempt to read two
> calendar pages for feed links returned nothing, so every "Feed" entry is *unverified*; (2) each
> site's terms of use and robots rules; (3) event freshness; (4) whether the owner permits reuse.
> Entries marked "URL not confirmed" were named by another page but not found directly.
>
> **Town scope:** the first five towns in the app's town list: Ashland, Framingham, Holliston,
> Hopkinton, Natick (all MA). Many other places are also called "Ashland" (OH, KY, VA, NH, TN, WI).
> Search results for those were excluded. Examples excluded: `visitashland.com`, `visitashland.org`,
> `ashlandchamber.com`, `ashlandcountyparks.com`. Any future connector must key sources by town
> **and state**.

### Source registry fields (what each row needs when this becomes data)
town + state · source name · source URL · platform/type · ingestion method · schedule · permission
status · terms reviewed (date) · last successful fetch · last error · trust level.

### A.1 Ashland, MA
| # | Source | URL | Platform / type | Content | Feed |
|---|--------|-----|-----------------|---------|------|
| A1 | Town calendar (all calendars) | https://www.ashlandmass.com/calendar.aspx?CID=0&view=list | CivicPlus calendar | Town events. Sub-calendars: Town Clerk `CID=24`, Council on Aging `CID=76`, Library | Unverified. CivicPlus sites often offer ICS/RSS |
| A2 | Public Library | https://ashland.librarycalendar.com/events/upcoming | LibraryCalendar | Storytimes, programs, family events | Unverified. Confirm it is Ashland **MA** (`ashlandne.` is a different library) |
| A3 | Recreation (registration) | https://ashlandma.myrec.com/info/activities/activities.aspx | MyRec | Seasonal programs, trips, sports | Unverified. Program listings, not one-off events |
| A4 | Schools community bulletin board | https://www.ashland.k12.ma.us/Flyers | School district site | Community flyers. The page says it updates every Friday | None expected. Flyers as web/PDF/image |
| A5 | Ashland Town News calendar | https://www.ashlandtownnews.com/calendar | Local news network (Locable) | Community calendar | Unverified. News-site terms apply |
| A6 | Patch Ashland calendar | https://patch.com/massachusetts/ashland-ma/calendar | News site calendar | Mixed local events | Unverified. News-site terms apply |
| A7 | Ashland Senior News (monthly) | https://container.parishesonline.com/bulletins/06/5054/20251101N.pdf (pattern: `YYYYMM01N.pdf`) | PDF newsletter | Senior center events | None. PDF text/OCR. Current-month filename not confirmed |
| A8 | Ashland State Park (DCR) | https://mass.gov/locations/ashland-state-park | State site | Seasonal park info, guided hikes | Low value for events |
| A9 | School-parent organizations directory | https://ashpac.ashland.k12.ma.us/resources/local-organizations | School PAC page | **Lead list** of local organizations, not events | n/a. Use to find more sources |

### A.2 Framingham, MA
| # | Source | URL | Platform / type | Content | Feed |
|---|--------|-----|-----------------|---------|------|
| F1 | City calendar | https://www.framinghamma.gov/Calendar.aspx | CivicPlus calendar | City and community events. Community Calendar is `CID=14`. Farmers Market is a recurring event (`EID=40296`) | Unverified |
| F2 | Public Library | https://framinghamlibrary.org/calendar | Library site (own calendar) | Programs at the Main Library and branch. Pages like `/calendar/68` | Unverified |
| F3 | Parks & Recreation registration | https://framinghamma.myrec.com/ | MyRec | Programs and registration | Unverified |
| F4 | Parks, Recreation & Cultural Affairs | https://www.framinghamma.gov/951/Parks-Recreation-Cultural-Affairs | City page | Parent of Parks, Recreation, Callahan Center (senior services), Loring Arena | n/a. Lead page for sub-sources |
| F5 | Access Framingham community calendar | https://www.accessfram.tv/cal | Community calendar | Open to public submissions | Unverified. User-submitted content needs quality checks |
| F6 | Danforth Art Museum | https://danforth.framingham.edu/visit/event-calendar/ | University museum site | Workshops, receptions, artist talks | Unverified |
| F7 | Framingham State University | https://www.framingham.edu (newsletter "Campus Currents") | University site | Campus and public events. Events page URL not confirmed | Unverified |

### A.3 Holliston, MA
| # | Source | URL | Platform / type | Content | Feed |
|---|--------|-----|-----------------|---------|------|
| H1 | Town website | https://www.townofholliston.us/ | CivicPlus (Agenda Center in use) | Town notices and meetings. Events calendar page URL not confirmed | Unverified |
| H2 | Public Library | https://hollistonlibrary.org/ | WordPress | Programs. Monthly newsletter as PDF. Events page URL not confirmed | Unverified |
| H3 | Holliston Town News calendar | https://www.hollistontownnews.com/ (monthly "Holliston Calendar" posts) | Local news network (Locable) | Monthly roundup of events across Holliston and nearby towns. Also event pages under `/events/` | Unverified. Free-text roundups need LLM extraction. News-site terms apply |
| H4 | Parks & Recreation | https://holliston.activityreg.com/clientpage_t2.wcs | ActiveReg | Programs and registration | Unverified |
| H5 | Patch Holliston-Hopkinton calendar | https://patch.com/massachusetts/holliston-hopkinton/calendar | News site calendar | Events for both towns (shared with Hopkinton) | Unverified |
| H6 | Holliston Farmers Market | https://thewsk.org/farmers-market-schedule | Community organization | Weekly market schedule | Unverified |
| H7 | Washington Street Players | https://www.washingtonstreetplayers.org/ | Theatre group | Performances at Town Hall | Unverified. Organizer lead |
| H8 | Metro West Collaborative (CASA) | https://www.mwc-casa.org | Regional organization | Named in the June roundup. Content not reviewed | Unverified. Organizer lead |

### A.4 Hopkinton, MA
| # | Source | URL | Platform / type | Content | Feed |
|---|--------|-----|-----------------|---------|------|
| K1 | Town website: meeting calendar | https://www.hopkintonma.gov/eGovernment1/meeting_calendar_and_agendas.php | Custom town CMS (not CivicPlus) | **Meetings only**, not community events | Unverified |
| K2 | Town Manager Weekly Update | https://www.hopkintonma.gov/news_detail_T13_R393.php (pattern `news_detail_T13_R###.php`) | Town news posts | **Weekly roundup of local events** (trail walks, arts, library, senior events) | None. Free-text pages. Needs LLM extraction |
| K3 | Public Library: adult events | https://hopkintonlibrary.org/for-adults/adult-events/ | WordPress | Adult programs | Unverified |
| K4 | Public Library: young-adult events | https://hopkintonlibrary.org/for-young-adults/young-adult-events/ | WordPress | Teen programs | Unverified |
| K5 | Public Library: children's events | https://hopkintonlibrary.org/for-kids/childrens-events/ | WordPress | Children's programs | Unverified |
| K6 | Patch Holliston-Hopkinton calendar | https://patch.com/massachusetts/holliston-hopkinton/calendar | News site calendar | Same page as H5 | Unverified |
| K7 | Hopkinton Center for the Arts | URL not confirmed | Arts center | Concerts, films, talks (named in K2 posts) | Unknown |
| K8 | Hopkinton Senior Center | URL not confirmed (28 Mayhew Street) | Town department | Senior events and resource fairs | Unknown |
| — | **Gap: Recreation Department** | not found | — | Searches did not return a recreation program page | — |

Excluded: `hopkintontownlibrary.org` appeared in results, but it was not shown to be Hopkinton **MA**.

### A.5 Natick, MA
| # | Source | URL | Platform / type | Content | Feed |
|---|--------|-----|-----------------|---------|------|
| N1 | Town calendar (all) | https://www.natickma.gov/Calendar.aspx | CivicPlus calendar | Town events. Sub-calendars: Natick Community Events `CID=117`, Recreation & Parks `CID=22`. Library events also appear here (`EID` pages) | Unverified |
| N2 | Morse Institute Library | https://www.natickma.gov/299/Morse-Institute-Library | Town page (library has its own site; domain not confirmed) | Library and senior-center programs | Unverified |
| N3 | Natick Historical Society events | https://algae-bell-hhg3.squarespace.com/events | Squarespace (temporary-looking host name) | Walks, talks | Unverified. Confirm the real domain first |
| N4 | The Natick Report | https://www.natickreport.com | Local news site | Local news and event items | None. News-site terms apply |
| N5 | Friends of the Morse Institute Library | https://morsefriends.org/blog | Friends group | Book sales | Unverified |
| N6 | Town-run events (Natick Days, Spooktacular, Natick Center Cultural District) | via N1 | Town calendar | Festivals and seasonal events | Same as N1 |

### A.6 What the list shows (for the P3 design)
- **One calendar connector covers many towns.** Ashland, Framingham, and Natick use the same CivicPlus
  calendar pattern. Holliston very likely does too (unconfirmed). Hopkinton does not. A reusable
  CivicPlus connector is the best first investment.
- **Recreation uses shared platforms:** MyRec (Ashland, Framingham) and ActiveReg (Holliston).
  Hopkinton and Natick have none found. These list *programs*, not one-off events, so they may need a
  different "program" type, not an "event" (a P3 design question).
- **Libraries are the most reliable event sources** in every town. Their platforms differ
  (LibraryCalendar, own site, WordPress), so library coverage needs several small connectors or a
  generic web extractor.
- **News and Patch calendars overlap** town sources. They add community events but also create
  duplicates and carry reuse restrictions. Treat them as **lead sources** (find organizers, check
  coverage) until the terms review says otherwise. Do not republish their text.
- **Free-text roundups** (Hopkinton's weekly update, Holliston Town News monthly calendar) need LLM
  extraction. They are rich in events but low in structure.
- **Coverage gaps found:** Hopkinton recreation, senior center, and arts center pages; Holliston
  calendar and library event URLs; Natick library's own site; Ashland has no chamber of commerce
  found. The MetroWest Chamber of Commerce (`https://www.metrowest.org`) serves Ashland, Framingham,
  Holliston, Hopkinton, and Natick, so it may have a regional event listing. Not reviewed.
- **Organizer-first leads:** school PAC organization lists, theatre groups, farmers markets, friends
  groups, and historical societies appeared repeatedly. Invited organizer submission (P1 follow-up)
  may cover these better than crawling.

### A.7 Version history (appendix)
- **2026-11-01:** Appendix added from web research for Ashland, Framingham, Holliston, Hopkinton, and
  Natick. 38 candidate rows (one is a duplicate of another: Patch Holliston-Hopkinton). No feeds, terms, or robots rules verified. This PRD's status is still
  `placeholder`.


---

## Appendix B — Seed source list for the other 23 towns (research, 2026-11-01)

> **Status: CANDIDATE list, same limits as Appendix A.** Nothing here was fetched by Arangly or
> approved. The four research workers used search results only and fetched no pages. Platform names
> come from page titles and URL patterns, so they are **inferred, not confirmed**.
>
> **Feeds:** every source is **feed-unverified** unless a row says otherwise. Exactly one feed link was
> seen in all of Appendix B (Cambridge, B.6). Terms of use and robots rules were not checked for any row.
> **Columns:** Source · URL · Platform / content. "URL not confirmed" means another page named the source
> but the address was not found. "Event page only" means the URL is one event, not the calendar root.
> Wrong-state results (for example the Greater Sudbury in Ontario) were excluded.

### B.1 Sherborn, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar | https://www.sherbornma.org/calendar.aspx | CivicPlus calendar. Town events and meetings |
| Council on Aging calendar | https://sherborncoa.org/coa-events/calendar/ | Separate COA site, one page per event. Book group, workshops |
| Library | https://sherbornlibrary.org/ | Library site. Arts & Crafts Fair, summer concerts |
| Library kids events | https://sherbornlibrary.org/kids/kids-events/ | Children's programs |
| Library exhibits | https://sherbornlibrary.org/news-events/exhibits/ | Art exhibits |
| Library calendar (old) | https://sherbornma.assabetinteractive.com/calendar/2024-may/ | Assabet Interactive. Only a 2024 page seen, may be stale |

Gaps: no Parks & Recreation, Patch page, farmers market, or historical society found.

### B.2 Wellesley, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar | https://wellesleyma.gov/Calendar.aspx | CivicPlus. Town events and meetings |
| Community Events category | https://wellesleyma.gov/calendar.aspx?CID=78 | CivicPlus category. Church and nonprofit events |
| Notify Me | https://wellesleyma.gov/list.aspx | Email alerts (not a feed) |
| Free Library events | https://www.wellesleyfreelibrary.org/blog/category/events/ | WordPress-style category. Library events |
| Recreation | https://wellesleyma.gov/1828/Recreation | Town CMS. Programs and registration dates |
| Recreation registration | https://wellesleyma.gov/1462/Register-Online | How to register |
| Council on Aging (MyActiveCenter) | https://wellesleyma.gov/891/MyActiveCenter | Senior program registration. The MyActiveCenter address itself not seen |
| Friends of Wellesley COA | https://wellesleyma.gov/Calendar.aspx?EID=9644 | Event page only |

Gaps: no Patch, Wellesley Society of Artists, chamber, or historical society found.

### B.3 Marlborough, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City calendar | https://www.marlborough-ma.gov/calendar | CivicPlus. Meetings and events |
| Notify Me | https://www.marlborough-ma.gov/notifyme | Email alerts (not a feed) |
| Library events | https://marlborough.librarycalendar.com/events/list | LibraryCalendar. Also /events/upcoming and /events/week |
| Library page | https://www.marlborough-ma.gov/157/Library | City page. Newsletter signup |
| Patch calendar | https://patch.com/massachusetts/marlborough/calendar | Patch |
| Farmers Market | https://marlboroughfarmersmarket.com/ | Standalone site. 2026 season confirmed by a Patch article |

Gaps: no Recreation site, COA events page, chamber, or arts venue found.

### B.4 Sudbury, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town site | https://sudbury.ma.us/ | WordPress multisite (inferred). Town news and events |
| Park and Recreation Commission | https://sudbury.ma.us/parkrecreationcommission/ | Meetings |
| Senior Center | https://sudburyseniorcenter.org/ | WordPress (inferred). Monthly newsletter PDFs |
| Goodnow Library | https://goodnowlibrary.org/ | WordPress (inferred). Events page address not seen |
| Friends of the Goodnow Library | https://www.friendsofgoodnow.org/ | Friends events, book sales |
| Patch calendar | https://patch.com/massachusetts/sudbury/calendar (likely root, not seen) | Patch. One event page seen |

Gaps: no town calendar page, recreation program site, chamber, or historical society found.

### B.5 Needham, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar | https://needhamma.gov/Calendar.aspx | CivicPlus. List view at /calendar.aspx?view=list |
| Park & Recreation registration | https://needhamprograms.myrec.com/info/default.aspx | MyRec |
| Park & Recreation (rec.us) | https://rec.us/organizations/needham | rec.us registration |
| Council on Aging | https://www.needhamma.gov/519/Council-on-Aging | Senior programs |
| Needham Compass newsletter | https://www.needhamma.gov/4601 | Bi-monthly COA newsletter |
| Free Public Library | https://needhamlibrary.org/ | WordPress (inferred). Monthly children's calendar PDFs |
| Patch calendar | https://patch.com/massachusetts/needham/calendar (likely root, not seen) | Patch. Event page only seen |

Gaps: no farmers market, historical society, chamber, or arts venue found.

### B.6 Dedham, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar | https://www.dedham-ma.gov/connect/calendar | Town CMS (not CivicPlus). Second view: /services/advanced-components/site-specific-pages/town-calendar |
| Community events | https://www.dedham-ma.gov/discover-dedham/community-events/-toggle-all | Searchable community events |
| Annual events | https://www.dedham-ma.gov/our-community/annual-events | Annual town events |
| Library events | https://events.dedhamlibrary.com/ | Probably LibNet (dedhamlibrary.libnet.info also seen) |
| Library site | https://dedhamlibrary.com/children/ | Children's programs, both branches |
| Parks and Recreation | https://www.dedham-ma.gov/town-departments/parks-and-recreation | Registration platform not confirmed |
| Council on Aging weekly activities | https://www.dedham-ma.gov/town-departments/council-on-aging/weekly-activities | Weekly senior activities |
| Friends of the DCOA newsletters | https://www.friendsofthedcoa.org/s/DCOA-Newsletter-June-2026-4f37.pdf | Monthly PDF |
| Patch calendar | https://patch.com/massachusetts/dedham/calendar | Patch |

Gaps: no farmers market, historical society, chamber, or arts venue found.

### B.7 Boston, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City events calendar | https://www.boston.gov/calendar | boston.gov. Page text says an RSS feed exists. **Link not seen** |
| Boston Public Library | https://www.bpl.org (branch pages like /locations/south-boston/) | Branch events. Central events page not confirmed |
| Parks: ParkARTS | https://content.boston.gov/departments/parks-and-recreation/parkarts | Free park events |
| Parks: sports and activities | https://www.boston.gov/departments/parks-and-recreation/parks-sports-and-activities | Drop-in programs |
| ArtsBoston calendar | URL not confirmed | Named by boston.gov as the arts calendar |
| Patch Boston calendar | https://patch.com/massachusetts/boston/calendar | User-submitted events |
| Patch neighborhoods | https://patch.com/massachusetts/jamaicaplain/calendar | Same pattern for Back Bay, Beacon Hill |
| Age Strong Commission | https://boston.gov/departments/age-strong-commission/events-and-engagement-older-adults | Senior centers (Brighton, East Boston) |

Gaps: no community or neighborhood group found. Boston needs a neighborhood-level source plan. One city list is too coarse.

### B.8 Cambridge, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Community Development calendar | https://www.cambridgema.gov/cdd/calendar | One calendar per department. **ICS link seen:** http://www.cambridgema.gov/CDD/Calendar.ics (this department only) |
| Human Service Programs calendar | https://www.cambridgema.gov/Departments/humanserviceprograms/Calendar | Recreation, senior, community programs. Category filters |
| Public Library | https://www.cambridgema.gov/departments/cambridgepubliclibrary | Monthly PDF newsletter. Live calendar not confirmed |
| Recreation | https://www.cambridgema.gov/Departments/HumanServicePrograms/Recreation | Programs |
| Cambridge Arts calendar | https://www.cambridgema.gov/arts/calendar | Cambridge Arts events only. Page says outside submissions are not accepted now |
| Cambridge Arts programs | https://www.cambridgema.gov/arts/Programs | River Festival, Porchfest |
| Patch calendar | https://patch.com/massachusetts/cambridge/calendar | User-submitted events |
| Cambridge Community Center | https://www.cambridgecc.org/ | Youth, family, senior. Events page not confirmed |

Gaps: no single city-wide calendar, library calendar, or senior center page found. **Cambridge is the only place where an ICS feed was actually seen.** Each department may have its own `.ics`.

### B.9 Somerville, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City events and calendar | https://www.somervillema.gov/events and /calendar | City site (not fetched). City events and COA classes |
| Past events | https://www.somervillema.gov/pastevents | Archive |
| Public Library calendar | https://somervillepubliclibrary.org/calendar/ | WordPress pattern. Monthly branch PDFs also exist |
| Library programs | https://somervillepubliclibrary.org/programs-events/ | All ages |
| Arts Council events | https://somervilleartscouncil.org/events/ | ArtBeat, SomerWrites |
| Patch calendar | https://patch.com/massachusetts/somerville/calendar | User-submitted events |
| Council on Aging | https://www.somervillema.gov/events/council-aging | Also in the main city calendar |
| Union Square Main Streets | https://www.unionsquaremain.org | Events page not confirmed |

Gaps: no parks and recreation events page found.

### B.10 Brookline, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar | https://www.brooklinema.gov/Calendar.aspx and ?view=list | CivicPlus |
| Public Library events | https://www.brooklinelibrary.org/events/event/ | Three locations. Platform not confirmed |
| Recreation / community education | https://www.brooklinema.gov/1545/Adult-Ed-Community-Education | CivicPlus page |
| Parks community events | https://www.brooklinema.gov/2048/Community-Events-Volunteer-Opportunities | Park cleanups, volunteering |
| ArtsBrookline calendar | URL not confirmed | Named on town's community organizations page |
| Patch calendar | https://patch.com/massachusetts/brookline/calendar (pattern, root not seen) | Patch |
| Senior Center / COA | https://www.brooklineseniorcenter.org and https://www.brooklinema.gov/245/Council-on-Aging | WordPress (inferred). Monthly newsletters |
| Notify Me | https://www.brooklinema.gov/list.aspx | Email list (not a feed) |

Gaps: ArtsBrookline address and Patch root not confirmed.

### B.11 Quincy, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Special events | https://quincyma.gov/specialevents/ | City festivals and events |
| City home page | https://www.quincyma.gov/ | Event notices. Full calendar not confirmed |
| Thomas Crane Public Library | https://thomascranelibrary.org | A calendar at thomascranelibrary.assabetinteractive.com/calendar/ showed only 2022–2024 entries. Live address not confirmed |
| Library children's PDFs | https://tcplquincy.org/wp-content/uploads/UPDATED-CHILD-Summer-2026.pdf | WordPress. Children's programs |
| Recreation | https://quincyma.myrec.com/info/activities/activities.aspx | MyRec |
| Patch calendar | https://patch.com/massachusetts/quincy/calendar | User-submitted events |
| Discover Quincy events | https://discoverquincy.com/events/ | Tourism site. Includes Quincy Historical Society |
| Council on Aging | URL not confirmed | Only meeting agendas seen |

Gaps: no city events calendar, arts office, or COA events page found. `quincyparkdistrict.com` omitted: not shown to be in MA.

### B.12 Newton, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Cultural Development events | https://www.newtonma.gov/government/parks-recreation-culture/cultural-development/events | Citywide festivals and fairs |
| City events landing | http://newtonma.gov/events | Community events |
| Historic Newton calendar | https://www.newtonma.gov/government/historic-newton/events/calendar | Museum and historic house events |
| Social Services events | https://www.newtonma.gov/government/health-human-services/social-services/events/-toggle-all | No content seen |
| Newton Free Library | https://newtonfreelibrary.net/ | Kids, teen, adult programs, exhibits |
| Library teens page | https://newtonfreelibrary.net/kids-tweens-teens/teens/ | Upcoming events list |
| Patch calendar | URL not confirmed | Patch |

Gaps: no Parks & Recreation registration, senior center, or farmers market found.

### B.13 Waltham, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City calendar | https://www.city.waltham.ma.us/Calendar.aspx?CID=23 and ?CID=0&view=list | CivicPlus |
| News Flash | https://www.city.waltham.ma.us/CivicAlerts.aspx?CID=1 | Event round-ups |
| Library calendar | https://waltham.assabetinteractive.com/calendar/ | Assabet Interactive (path seen in a listing) |
| Library directory page | https://www.minlib.net/our-libraries/waltham | Minuteman Library Network |
| Patch calendar | https://patch.com/massachusetts/waltham/calendar | Event page only seen. Also lists Lexington, Newton, Watertown events |
| Recreation Department | URL not confirmed | Programs |

Gaps: no Council on Aging, farmers market, or recreation registration platform found.

### B.14 Watertown, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City events calendar | URL not confirmed | Linked from the visitors page |
| Free Public Library events | https://watertownlib.org/events | Programs |
| Library special events | https://watertownlib.org/special-events | Community read |
| Senior Services | https://www.watertown-ma.gov/284/Senior-Services | CivicPlus-style paths |
| Senior newsletters | https://watertown-ma.gov/296/Newsletters | Monthly PDFs |
| Recreation | https://www.recreation.watertown-ma.gov/residents?contentId=3ddad414-3e7d-46bd-a991-5d98cc50f46e | Recreation subsite. Programs and brochures |
| Farmers Market (PDF) | https://www.watertown-ma.gov/asset/70df0ae6-8116-463a-85dd-1c8a5d091228 | Wednesdays Jun 10 – Oct 21, 2026 |
| Patch calendar | https://patch.com/massachusetts/watertown/calendar | Patch |

Gaps: no chamber, arts venue, or historical society found.

### B.15 Lexington, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar | https://www.lexingtonma.gov/Calendar/home and https://lexingtonma.gov/Calendar.aspx | CivicPlus |
| Recreation calendar | https://lexingtonma.gov/calendar.aspx?CID=25 | CivicPlus category |
| Recreation registration | https://lexrecma.myrec.com/ | MyRec (seen as a subpath) |
| Recreation page | https://www.lexingtonma.gov/511/Recreation-Community-Programs | Brochures |
| Cary Library | https://www.lexingtonma.gov/Facilities/Facility/Details/Cary-Library-30 | Library events also appear in the town calendar |
| Senior Services | https://www.lexingtonma.gov/354/Senior-Services | Senior center |
| Munroe Center for the Arts | https://www.munroecenter.org/events | Arts events |

Gaps: no separate Cary Library events site, chamber, or historical society found.

### B.16 Arlington, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar (example event) | https://www.arlingtonma.gov/Home/Components/Calendar/Event/39105/ | "Components/Calendar" path. Landing page not found |
| Robbins Library | https://www.robbinslibrary.org/events and /events/list/ | Library programs |
| Friends of the Robbins Library | https://friendsofrobbinslibrary.org/whats-new/ | Films and talks |
| Council on Aging | https://www.arlingtonma.gov/departments/health-human-services/council-on-aging | Senior programs |
| COA programs | https://www.arlingtonma.gov/departments/health-human-services/council-on-aging/social-services-programs-and-activities | Programs and appointments |
| Recreation | https://arlingtonma.myrec.com/ | MyRec |
| Patch | URL not confirmed | Only an article seen |

Gaps: no town calendar landing page, farmers market, chamber, or historical society found.

### B.17 Medford, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City website | https://www.medfordma.org/ | Finalsite. Events calendar mentioned in a PDF |
| City alerts | https://www.medfordma.org/about/alerts | Email alerts (not a feed) |
| Council on Aging / Senior Center | https://medfordma.org/departments/council-on-aging/ | Senior calendar |
| Recreation | https://medfordma.myrec.com/ | MyRec (seen as a subpath) |
| Public Library (state directory) | https://libraries.state.ma.us/library/medford-charlotte-and-william-bloomberg-medford-public-library | Library's own events address not confirmed |
| Friends of Medford Library | https://friendsofmedfordlibrary.org/ | Friends events |
| CACHE in Medford calendar | https://www.cacheinmedford.org/event-calendar/list/ | Local arts, library, community events. Strong aggregator candidate |
| CACHE: library venue | https://cacheinmedford.org/venue/medford-public-library/ | Library events |

Gaps: no city events calendar, library events page, farmers market, chamber, or historical society found.

### B.18 Malden, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City calendar (Main, CID=14) | https://cityofmalden.org/calendar.aspx?CID=14 | CivicPlus |
| All calendars | https://cityofmalden.org/calendar.aspx?CID=0&view=list | Includes board meetings |
| Teen Center calendar | https://cityofmalden.org/Calendar.aspx?CID=24 | Teen events |
| Public Library events | https://maldenpubliclibrary.org/browse-mpl/events-programs/ | Classes and events, all ages |
| Recreation | https://maldenma.myrec.com/info/activities/activities.aspx | MyRec |
| MaldenEvents.com | https://www.maldenevents.com/ | Central community calendar (Urban Media Arts) |
| Senior Center events | https://www.maldenevents.com/Center | Events for ages 60+ |
| Patch | event page only: https://patch.com/massachusetts/malden/calendar/event/20230311/449e4e6e-4c73-4d67-8a1d-f239982c3dac/filmbuilding-malden-info-session | Root not seen |
| Rivers Edge | https://riversedgema.com/calendar/ | Development events |

Gaps: no Patch root, chamber, farmers market, or historical society found.

### B.19 Milton, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town website | http://www.miltonma.gov/1 | Calendar page not confirmed |
| Library upcoming events | https://miltonlibrary.org/events/upcoming-events/ | Programs, author talks, music |
| Library Speaker Series | https://miltonlibrary.org/events/speaker-series/ | Author events |
| Library "Milton Grows" | https://miltonlibrary.org/events/milton-grows/ | Gardening and sustainability |
| Patch calendar | https://patch.com/massachusetts/milton/calendar | Patch |
| Milton Times "Happenings" | https://www.miltontimes.com/happenings/ | Local newspaper. Terms apply |
| COA directory entry | https://members.mcoaonline.org/directory/Details/milton-council-on-aging-3483778 | Contact details only. Not an events page |

Gaps: no town calendar, recreation registration, or COA events page found. Results for `milton.ca` and `miltonga.gov` were other places and excluded.

### B.20 Braintree, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town website | https://www.braintreema.gov/ | CivicPlus (CivicAlerts seen). Mentions a town calendar |
| Recreation & Community Events | https://braintreema.gov/directory.aspx?did=31 | Department contacts |
| Recreation registration | https://braintreema.myrec.com/ | MyRec |
| Thayer Public Library events | https://www.thayerpubliclibrary.org/events/ | WordPress. Registration links go to a LibCal site (thayerpubliclibrary.libcal.com) |
| Thayer monthly newsletter | https://www.thayerpubliclibrary.org/wp-content/uploads/2026/07/newsletter-6_2026.pdf | One PDF per month |
| Historical Society | https://braintreehistoricalsociety9.wildapricot.org/ | Wild Apricot |
| Patch | event page only (root not seen) | Patch |
| Elder Affairs | URL not confirmed | Senior events |

Gaps: no Elder Affairs page, chamber, or arts venue found.

### B.21 Woburn, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City events | https://woburnma.gov/events/ | WordPress-style with an events plugin. Free public events |
| Public Library | http://www.woburnpubliclibrary.org/ | WordPress. Upcoming events on home page |
| Library kids programs | https://woburnpubliclibrary.org/kids/programs/ | Children's events |
| Recreation registration | https://woburnma.myrec.com/info/activities/activities.aspx | MyRec |
| Recreation Department | https://woburnma.gov/government/recreation/ | Department page |
| Senior Center | https://woburnma.gov/government/senior/ | Senior programs |
| Patch calendar | https://patch.com/massachusetts/woburn/calendar | Some listings are in other towns |

Gaps: no chamber, historical society, farmers market, or arts venue found.

### B.22 Burlington, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| Town calendar | https://bpd.org/calendar.aspx?CID=14 | CivicPlus. **Domain is `bpd.org`, not town-named. Confirm it is the town's site before use** |
| Library events | https://burlington.librarycalendar.com/events/upcoming | LibraryCalendar |
| Library (older) | https://burlington.assabetinteractive.com/calendar/ | Assabet Interactive. Probably older |
| Parks & Recreation | https://maburlingtonweb.myvscloud.com/ | Vermont Systems registration |
| COA newsletter | https://container.mycommunityonline.com/bulletins/06/5073/20250901N.pdf | Monthly PDF |
| Chamber of Commerce | https://business.burlingtonchamberofcommerce.org/events/calendar | ChamberMaster. Business and community events |
| Burlington Access TV events | https://bcattv.org/events/ | Town events, Historical Society talks |
| Patch | event page only (root not seen) | Patch |

Gaps: no Historical Society site, farmers market, or arts venue found.

### B.23 Peabody, MA
| Source | URL | Platform / content |
|--------|-----|--------------------|
| City calendar | https://peabody-ma.gov/full-calendarG3.html | Custom city site. Meetings and events |
| City website (second domain) | https://www.peabodyma.gov/ | **Two domains seen. Confirm which one is official** |
| Peabody Institute Library | URL not confirmed. State directory lists events: https://libraries.state.ma.us/library/pu-peabody-institute | |
| Council on Aging blog | https://peabodycoa.blogspot.com/ | Blogger. Senior events |
| COA monthly newsletter | http://www.peabodycoa.org/news/April_2026.pdf | One PDF per month |
| Historical Society programs | https://peabodyhistorical.org/programs-exhibits/ | Walking tours, exhibits |
| Patch | event page only (root not seen) | Patch |
| Patch library news | https://patch.com/massachusetts/peabody/peabody-host-free-summer-eats-lunch-program-starting-july-6 | Article, not a calendar |

Gaps: no recreation site, farmers market, or Main Streets found. Danvers results were excluded (different town).

### B.24 What Appendix B adds (for the P3 design)
- **CivicPlus calendars are the most common platform.** Seen at: Sherborn, Wellesley, Marlborough,
  Needham, Brookline, Waltham, Lexington, Malden, Braintree, Burlington (domain to confirm).
  Together with Appendix A (Ashland, Framingham, Natick), that is **13 of 28 towns** on one calendar
  pattern. **One CivicPlus connector is the best first build.** Each town needs only a registry row.
- **MyRec appears in 10 towns:** Ashland, Framingham, Needham, Quincy, Lexington, Arlington, Medford,
  Malden, Braintree, Woburn. Recreation data is programs and registrations. P3 must decide
  whether "program" is an event.
- **Library platforms vary:** LibraryCalendar (Marlborough, Burlington, Ashland), Assabet Interactive
  (Sherborn, Quincy, Waltham, Burlington, some stale), WordPress sites (several), LibNet (Dedham).
  Libraries are valuable. They need a small connector for each platform.
- **Patch calendars exist for most towns.** In many cases only one event page was seen, not the calendar
  root. Patch content is user-submitted and has reuse limits. Keep it as a lead source.
- **Only one feed was confirmed:** Cambridge's `CDD/Calendar.ics`. Boston says an RSS feed exists but the
  link was not seen. Every other feed claim needs a direct check. This is the first task for the
  registry build.
- **Non-CivicPlus city sites** (Boston, Cambridge, Somerville, Dedham, Newton, Medford, Woburn, Peabody,
  Quincy, Arlington) each need a different connector or a general web extractor.
- **Aggregators worth review:** CACHE in Medford, MaldenEvents.com, Access Framingham, and the
  MetroWest Chamber of Commerce. One aggregator may replace many separate sources, but it also brings
  duplicates and reuse questions.
- **Large cities need a plan by neighborhood.** Boston, Cambridge, Somerville, and Quincy have too many
  sources for one row each. The registry should allow several sources per town and a neighborhood tag.
- **PDF newsletters** from senior centers and libraries appear in many towns (Ashland, Dedham,
  Needham, Brookline, Watertown, Burlington, Peabody, Braintree). They need PDF text extraction.
- **Domain checks needed:** Burlington (`bpd.org`) and Peabody (two city domains).

### B.25 Version history (appendix B)
- **2026-11-01:** Appendix B added from web research on the 23 other towns in the app's town list.
  172 rows. Search results only, no pages fetched. One feed link seen (Cambridge). No terms or robots
  rules checked. This PRD's status is still `placeholder`.


---

## Appendix C — Implementation priority order for towns (driver instruction, 2026-11-01)

> **Rule:** towns are taken up for source research, verification, connector work, and onboarding in order of
> **straight-line distance from Ashland, MA, nearest first** (ties by name).
> **Scope of this rule:** the order of **implementation work only**. It does **not** rank towns anywhere a
> visitor sees them (not search results, not the landing list, not the sitemap). Visitor-facing behavior is
> specified in the P2.1 PRD and is unchanged by this rule.
> **Why it matters:** Ashland is the pilot home town. Work that is closest to it is exercised first and reaches
> the most useful neighbors first.

Distances are straight-line (haversine) from Ashland's point (42.2539, −71.4583), computed from the town
centre points in the app's town list on 2026-11-01. Platform notes come from Appendices A and B and are
**unverified**.

| Priority | Town | Miles from Ashland | Calendar platform seen (unverified) | Researched in |
|---------:|------|-------------------:|-------------------------------------|---------------|
| 1 | Ashland | 0.0 | CivicPlus | A.1 |
| 2 | Framingham | 3.6 | CivicPlus | A.2 |
| 3 | Holliston | 3.7 | CivicPlus likely (events page not confirmed) | A.3 |
| 4 | Sherborn | 4.3 | CivicPlus | B.1 |
| 5 | Hopkinton | 4.4 | Custom town CMS (not CivicPlus) | A.4 |
| 6 | Natick | 5.7 | CivicPlus | A.5 |
| 7 | Marlborough | 8.0 | CivicPlus | B.3 |
| 8 | Sudbury | 9.1 | WordPress town site, no calendar page found | B.4 |
| 9 | Wellesley | 10.2 | CivicPlus | B.2 |
| 10 | Needham | 10.8 | CivicPlus | B.5 |
| 11 | Waltham | 14.8 | CivicPlus | B.13 |
| 12 | Dedham | 15.1 | Custom town CMS | B.6 |
| 13 | Newton | 15.4 | City site, platform not confirmed | B.12 |
| 14 | Watertown | 16.4 | Platform not confirmed, calendar page not found | B.14 |
| 15 | Brookline | 17.3 | CivicPlus | B.10 |
| 16 | Lexington | 17.6 | CivicPlus | B.15 |
| 17 | Arlington | 19.2 | Other calendar component | B.16 |
| 18 | Milton | 19.5 | Town calendar not found | B.19 |
| 19 | Cambridge | 19.6 | Per-department calendars, one ICS link seen | B.8 |
| 20 | Somerville | 20.2 | City site, platform not confirmed | B.9 |
| 21 | Medford | 21.1 | Finalsite, calendar not found | B.17 |
| 22 | Boston | 21.2 | boston.gov, RSS mentioned but link not seen | B.7 |
| 23 | Burlington | 22.0 | CivicPlus (domain `bpd.org` to confirm) | B.22 |
| 24 | Woburn | 22.1 | WordPress-style events page | B.21 |
| 25 | Quincy | 23.6 | Calendar not confirmed | B.11 |
| 26 | Malden | 23.6 | CivicPlus | B.18 |
| 27 | Braintree | 23.8 | CivicPlus | B.20 |
| 28 | Peabody | 31.8 | Custom city site | B.23 |

**Notes**
- **Sherborn is priority 4.** The first research pass took "the first five towns" in the app's list order
  (Ashland, Framingham, Holliston, Hopkinton, Natick). Sherborn sits between Holliston and Hopkinton by
  distance, and it was researched in the second pass (B.1). Its sources are already listed.
- **Ties:** Quincy and Malden are both 23.6 miles at one decimal place. They are ordered by name. The tie
  order is not meaningful.
- **Beyond these 28 towns:** any town added later takes its place by the same rule.
- **Cheap wins by platform:** one CivicPlus connector would serve priorities 1, 2, 4, 6, 7, 9, 10, 11, 15, 16,
  23, 26, 27 (and likely 3). The distance order and the platform grouping can disagree. The driver instruction is
  the distance order. If a platform grouping would save a lot of work, the SDD should raise it as a question
  and not change the order silently.
- **Not a product ranking:** this table is not read by the application. If a future feature needs a per-town
  rollout order in the product (for example to decide which towns go live first), it will be specified as data
  in its own PRD.

### C.1 Version history (appendix C)
- **2026-11-01:** Appendix C added. The driver's instruction "rank towns by distance from Ashland" means the
  order of implementation work only. An earlier reading, which ranked towns in search results and on the
  landing page, was applied to the P2.1 PRD, TRD, and SDD by mistake and then fully reversed the same day
  (PRD v1.3, TRD v0.4, SDD v0.3 are the unchanged baselines).
