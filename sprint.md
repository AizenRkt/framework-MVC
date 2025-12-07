Sprint 5 : 
- Atao afaka mandefa valeur en attribut ana HttpServletRequest amin'ny alalan'ilay classe ModelView 

Sprint 3-bis
-Atao afaka micapture an'ilay url misy pattern "/zavatra/{valeur}" reny ilay framework,izany hoe na "/zavatra/ok" na "/zavatra/12" dia tokony hitany foana raha mappé ao amin'ilay projet de test ilay "/zavatra/{valeur}"

Sprint 6 : 
- Asiana arguments amin'izay ilay méthodes mappena amin'ilay annotation @UrlMapping iny dia ilay arguments asaina tadiaviny ao amin'ny requête nahatongavana tany @le endpoint (request.getParameter({anaranle-argument})


Sprint 6-bis : 
Mamorona annotation @RequestParam ho an'ilay arguments anle fonction endpoint dia ilay valeur ao anatiny no clé hitadiavana anle paramètre anatinle requête 
Ohatra hoe raha misy 
voidfonctionTest(@RequestParam("nbr") int nombre) dia tadiavina @ request.getParameter("nbr") ilay valeur tokony hi-invoke-na anle méthode fonctionTest

Sprint 6-ter : 
Atao afaka mirécupère an'ilay {valeur} ao anatin'ilay URL ilay framework dia lasa ireny (raha misy) no injecter-na ao amin'ilay méthode endpoint eo amin'ny placen'ilay argument mitovy anarana aminy
Ohatra hoe fonctionTest(int id) izany mitady {id} ao amle URL dia izay valeur hitany ao no ampiasainy
Dia mba hanamoraina ny fiainana amty ray ty ozy Mr Naina hoe asio ordre de priorité fotsiny ohatra hoe raha tss anle variable anaty url dia tadiavo anaty request ilay variable sinon soloy null na mithrow-eva Exception (exemple ana ordre io anah io fa izay tianareo)