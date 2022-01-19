import express from "express";
import { registerAccount, getNameByKbtId, postSaveNameByKbtId} from "./controllers";

const router: express.Router = express.Router();

//router.get("/updatekbt/:kbtid/:kbturl", saveNameByKbtId);
router.post("/register", registerAccount);
router.post("/updatekbt", postSaveNameByKbtId);
router.get("/kbt/:kbtid", getNameByKbtId);

export default router;
